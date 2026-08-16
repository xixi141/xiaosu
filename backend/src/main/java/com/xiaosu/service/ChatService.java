package com.xiaosu.service;

import com.xiaosu.config.AppProperties;
import com.xiaosu.dto.*;
import com.xiaosu.entity.ChatLogEntity;
import com.xiaosu.memory.ChatMemoryManager;
import com.xiaosu.memory.ChatSessionService;
import com.xiaosu.rag.CitationAssembler;
import com.xiaosu.rag.RagContext;
import com.xiaosu.rag.RefusalGuard;
import com.xiaosu.repository.ChatLogRepository;
import com.xiaosu.tool.ToolRecorder;
import com.xiaosu.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.ToolCallAdvisor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.DefaultToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 问答编排核心：拒答预检 → RAG 检索（引用）→ 手动拼接多轮历史（会话隔离）→ 工具回调（模型自主决策）
 * → DeepSeek 生成 → 全链路落 chat_log。流式与非流式共用同一条构建链。
 *
 * 关键设计（踩坑记录见 AI_USAGE.md）：
 * 1. ToolCallAdvisor 必须显式加入 advisor 链，且 conversationHistoryEnabled=false
 * 2. 不用 MessageChatMemoryAdvisor（会破坏工具循环内部调用的消息列表），历史消息手动拼接
 * 3. 工具上下文经 ToolCallingChatOptions 传递（请求级 .toolContext() 不会被读取）
 */
@Service
@Slf4j
public class ChatService {

    static final String SYSTEM_PROMPT = """
            你是「小苏」，公司的内部 AI 助手。
            回答规则（按优先级从高到低）：
            1. 涉及员工信息（工号查询）、考勤、订单、当前时间/日期的问题，必须调用对应工具查询，
               不得用知识库内容代替；回答时说明数据来源。
            2. 其他问题优先依据【知识库】内容回答，并在引用处标注编号，如 [1] 或 [1][2]；
               知识库内容与问题无关时忽略它。
            3. 知识库中没有相关内容时，直接说「根据知识库资料，没有找到相关信息」，严禁编造。
            4. 涉及个人隐私或未公开经营数据的问题，一律拒绝回答。
            5. 回答简洁、口语化、中文；多轮对话注意结合前面对话的上下文。
            """;

    private final ChatClient chatClient;
    private final ToolCallbackProvider toolCallbackProvider;
    private final ToolCallingManager toolCallingManager;
    private final VectorStoreService vectorStoreService;
    private final RefusalGuard refusalGuard;
    private final CitationAssembler citationAssembler;
    private final ChatLogRepository chatLogRepository;
    private final ChatMemoryManager chatMemoryManager;
    private final ChatSessionService chatSessionService;
    private final LlmGateway llmGateway;
    private final AppProperties props;

    public ChatService(ChatClient chatClient,
                       ToolCallbackProvider toolCallbackProvider,
                       ToolCallingManager toolCallingManager,
                       VectorStoreService vectorStoreService,
                       RefusalGuard refusalGuard,
                       CitationAssembler citationAssembler,
                       ChatLogRepository chatLogRepository,
                       ChatMemoryManager chatMemoryManager,
                       ChatSessionService chatSessionService,
                       LlmGateway llmGateway,
                       AppProperties props) {
        this.chatClient = chatClient;
        this.toolCallbackProvider = toolCallbackProvider;
        this.toolCallingManager = toolCallingManager;
        this.vectorStoreService = vectorStoreService;
        this.refusalGuard = refusalGuard;
        this.citationAssembler = citationAssembler;
        this.chatLogRepository = chatLogRepository;
        this.chatMemoryManager = chatMemoryManager;
        this.chatSessionService = chatSessionService;
        this.llmGateway = llmGateway;
        this.props = props;
    }

    /** 非流式问答（钉钉与后台日志用同一条链路） */
    public ChatResponseDto ask(ChatRequest req) {
        long start = System.currentTimeMillis();
        var refusal = refusalGuard.check(req.question());
        if (refusal.isPresent()) {
            ChatResponseDto resp = ChatResponseDto.refused(refusal.get());
            saveLog(req, resp, 0, start, ChatLogEntity.Status.REFUSED, null);
            return resp;
        }

        RagContext ctx = retrieve(req.question());
        ToolRecorder recorder = new ToolRecorder();
        String sessionKey = sessionKeyOf(req);
        try {
            ChatResponse response = buildClient().prompt(buildPrompt(ctx, sessionKey, req, recorder))
                    .call()
                    .chatResponse();
            String answer = response.getResult().getOutput().getText();
            Usage usage = response.getMetadata().getUsage();
            long latency = System.currentTimeMillis() - start;
            // 保存干净对话进记忆（user 提问 + 不含工具调用的最终回答）
            chatMemoryManager.save(sessionKey, new UserMessage(req.question()), new AssistantMessage(answer));
            ChatResponseDto resp = new ChatResponseDto(
                    answer,
                    citationAssembler.fromHits(ctx.hits()),
                    recorder.calls(),
                    ChatResponseDto.UsageInfo.from(usage),
                    "SUCCESS");
            saveLog(req, resp, latency, start, ChatLogEntity.Status.SUCCESS, null);
            return resp;
        } catch (Exception e) {
            log.error("问答失败: {}", req.question(), e);
            ChatResponseDto resp = ChatResponseDto.fallback("小苏现在无法连接大脑（服务暂时不可用），请稍后再试");
            saveLog(req, resp, System.currentTimeMillis() - start, start, ChatLogEntity.Status.FALLBACK, e.getMessage());
            return resp;
        }
    }

    /** 流式问答（Web 调试聊天页 SSE） */
    public Flux<StreamEvent> stream(ChatRequest req) {
        long start = System.currentTimeMillis();
        var refusal = refusalGuard.check(req.question());
        if (refusal.isPresent()) {
            ChatResponseDto resp = ChatResponseDto.refused(refusal.get());
            saveLog(req, resp, System.currentTimeMillis() - start, start, ChatLogEntity.Status.REFUSED, null);
            return Flux.just(StreamEvent.error(resp.answer()), StreamEvent.done(resp.usage(), List.of(), "REFUSED"));
        }

        RagContext ctx = retrieve(req.question());
        ToolRecorder recorder = new ToolRecorder();
        StringBuilder answer = new StringBuilder();
        AtomicReference<Usage> usageRef = new AtomicReference<>();
        List<Citation> citations = citationAssembler.fromHits(ctx.hits());
        String sessionKey = sessionKeyOf(req);

        Flux<ChatResponse> raw;
        try {
            raw = llmGateway.stream(buildClient(), buildPrompt(ctx, sessionKey, req, recorder));
        } catch (Exception e) {
            return fallbackFlux(req, start, e);
        }

        return Flux.concat(
                        Flux.just(StreamEvent.meta(citations)),
                        raw.concatMap(response -> {
                            usageRef.set(response.getMetadata().getUsage());
                            String delta = response.getResult().getOutput().getText();
                            if (delta != null) {
                                answer.append(delta);
                            }
                            return Flux.just(StreamEvent.token(delta == null ? "" : delta));
                        }),
                        Flux.defer(() -> {
                            long latency = System.currentTimeMillis() - start;
                            chatMemoryManager.save(sessionKey, new UserMessage(req.question()),
                                    new AssistantMessage(answer.toString()));
                            ChatResponseDto resp = new ChatResponseDto(answer.toString(), citations,
                                    recorder.calls(), ChatResponseDto.UsageInfo.from(usageRef.get()), "SUCCESS");
                            saveLog(req, resp, latency, start, ChatLogEntity.Status.SUCCESS, null);
                            return Flux.just(StreamEvent.done(resp.usage(), resp.toolCalls(), "SUCCESS"));
                        }))
                .onErrorResume(e -> {
                    log.error("流式问答失败: {}", req.question(), e);
                    return fallbackFlux(req, start, e);
                });
    }

    private Flux<StreamEvent> fallbackFlux(ChatRequest req, long start, Throwable e) {
        ChatResponseDto resp = ChatResponseDto.fallback("小苏现在无法连接大脑（服务暂时不可用），请稍后再试");
        saveLog(req, resp, System.currentTimeMillis() - start, start, ChatLogEntity.Status.FALLBACK, e.getMessage());
        return Flux.just(StreamEvent.error(resp.answer()), StreamEvent.done(resp.usage(), List.of(), "FALLBACK"));
    }

    /** 手动拼接 Prompt：system + 历史干净对话 + 当前提问，工具上下文放 ToolCallingChatOptions */
    private Prompt buildPrompt(RagContext ctx, String sessionKey, ChatRequest req, ToolRecorder recorder) {
        String system = SYSTEM_PROMPT + "\n\n【知识库】\n" + ctx.contextText();
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(system));
        messages.addAll(chatMemoryManager.historyFor(sessionKey));
        messages.add(new UserMessage(req.question()));
        return new Prompt(messages, DefaultToolCallingChatOptions.builder()
                .toolContext(Map.of(ToolRecorder.TOOL_CONTEXT_KEY, recorder))
                .build());
    }

    /** 组装 ChatClient：只挂 ToolCallAdvisor，记忆由 ChatService 手动管理。
     *  conversationHistoryEnabled 保持默认 true：DeepSeek 要求内部调用的消息里
     *  tool 消息前必须有 assistant tool_calls 消息（false 时 Spring AI 只发
     *  [system, tool结果]，DeepSeek 直接 400——实测坑 #4 完整链路）。 */
    private ChatClient buildClient() {
        var toolCallAdvisor = ToolCallAdvisor.builder()
                .toolCallingManager(toolCallingManager)
                .build();
        return chatClient.mutate()
                .defaultAdvisors(toolCallAdvisor)
                .defaultToolCallbacks(toolCallbackProvider)
                .build();
    }

    private RagContext retrieve(String question) {
        List<RagContext.RagHit> hits = vectorStoreService.search(question);
        String contextText = citationAssembler.buildContextText(hits);
        log.debug("检索命中 {} 条: {}", hits.size(), question);
        return new RagContext(hits, contextText);
    }

    private String sessionKeyOf(ChatRequest req) {
        // Web 调试聊天：sessionId 即会话维度；钉钉：sessionId 已是 staffId#conversationId 格式
        return req.userId() == null || req.userId().isBlank()
                ? req.sessionId()
                : chatSessionService.sessionKeyOf(req.userId(), req.sessionId());
    }

    private void saveLog(ChatRequest req, ChatResponseDto resp, long latency, long start,
                         ChatLogEntity.Status status, String error) {
        // 注意：局部变量不能命名为 log（会遮蔽 Lombok @Slf4j 生成的 log 字段）
        ChatLogEntity entry = new ChatLogEntity();
        entry.setSessionId(req.sessionId());
        entry.setUserId(req.userId());
        entry.setQuestion(req.question());
        entry.setAnswer(resp.answer());
        entry.setModel(props.modelNameOrDefault());
        entry.setInputTokens(resp.usage().inputTokens());
        entry.setOutputTokens(resp.usage().outputTokens());
        entry.setTotalTokens(resp.usage().totalTokens());
        entry.setToolCalls(JsonUtil.toJson(resp.toolCalls()));
        entry.setCitations(JsonUtil.toJson(resp.citations()));
        entry.setIsRefused(status == ChatLogEntity.Status.REFUSED);
        entry.setStatus(status);
        entry.setErrorMessage(error);
        entry.setLatencyMs(latency);
        entry.setCreatedAt(LocalDateTime.now());
        chatLogRepository.save(entry);
        log.info("对话日志已记录: session={} status={} tokens={} latency={}ms",
                req.sessionId(), status, resp.usage().totalTokens(), latency);
    }
}
