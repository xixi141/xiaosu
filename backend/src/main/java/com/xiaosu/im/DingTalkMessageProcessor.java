package com.xiaosu.im;

import com.xiaosu.dto.ChatRequest;
import com.xiaosu.dto.ChatResponseDto;
import com.xiaosu.memory.ChatSessionService;
import com.xiaosu.service.ChatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/** IM 消息的异步业务处理：同步 ACK 后的实际工作（防止回调超时与消息堆积） */
@Service
@Slf4j
public class DingTalkMessageProcessor {

    private final ChatService chatService;
    private final ChatSessionService chatSessionService;
    private final DingTalkReplyService replyService;

    public DingTalkMessageProcessor(ChatService chatService,
                                    ChatSessionService chatSessionService,
                                    DingTalkReplyService replyService) {
        this.chatService = chatService;
        this.chatSessionService = chatSessionService;
        this.replyService = replyService;
    }

    @Async("imExecutor")
    public void process(String senderStaffId, String conversationId, String text, String sessionWebhook) {
        try {
            // 会话隔离键 = staffId#conversationId（验收 7.3：A 的上下文不能被 B 接到）
            String sessionKey = chatSessionService.sessionKeyOf(senderStaffId, conversationId);
            ChatResponseDto resp = chatService.ask(new ChatRequest(sessionKey, senderStaffId, text));
            replyService.sendMarkdown(sessionWebhook, "小苏", formatAnswer(resp));
        } catch (Exception e) {
            log.error("IM 消息处理失败", e);
            // 兜底：本地写死文案，不依赖 LLM（验收 7.5：坏 key 时用户不能一直转圈）
            replyService.sendMarkdown(sessionWebhook, "小苏",
                    "小苏现在无法连接大脑（服务暂时不可用），请稍后再试。\n\n如持续失败请联系管理员。");
        }
    }

    /** 回答 + 引用来源列表（钉钉 Markdown 卡片） */
    private String formatAnswer(ChatResponseDto resp) {
        StringBuilder md = new StringBuilder(resp.answer());
        if (!resp.citations().isEmpty()) {
            md.append("\n\n**📚 来源**\n");
            for (int i = 0; i < resp.citations().size(); i++) {
                var c = resp.citations().get(i);
                md.append(i + 1).append(". ").append(c.filename())
                  .append("（切片 #").append(c.chunkIndex()).append("）\n");
            }
        }
        return md.toString();
    }
}
