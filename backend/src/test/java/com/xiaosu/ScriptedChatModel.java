package com.xiaosu;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import reactor.core.publisher.Flux;

import java.lang.reflect.Constructor;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;

/**
 * 按脚本出牌的假 LLM：call/stream 依次吐出预设 ChatResponse。
 * 第一轮返回 tool_call（触发 Spring AI 执行真实工具），第二轮返回最终回答。
 *
 * 注意：AssistantMessage 带 toolCalls 的构造器是 protected 且类为 final，
 * 测试只能走反射构造（Spring AI 1.1.8 没有公开的测试工具类）。
 */
public class ScriptedChatModel implements ChatModel {

    private final Deque<ChatResponse> script;

    public ScriptedChatModel(List<ChatResponse> responses) {
        this.script = new ArrayDeque<>(responses);
    }

    @Override
    public ChatResponse call(Prompt prompt) {
        if (script.isEmpty()) {
            throw new IllegalStateException("脚本已耗尽");
        }
        return script.poll();
    }

    @Override
    public Flux<ChatResponse> stream(Prompt prompt) {
        return Flux.fromIterable(script);
    }

    @Override
    public ChatOptions getDefaultOptions() {
        return ChatOptions.builder().build();
    }

    /** 造一轮带 tool_call 的响应 */
    public static ChatResponse toolCallResponse(String callId, String toolName, String argumentsJson) {
        AssistantMessage.ToolCall toolCall = new AssistantMessage.ToolCall(callId, "function", toolName, argumentsJson);
        AssistantMessage message = newAssistantMessageWithToolCalls(List.of(toolCall));
        return new ChatResponse(List.of(new Generation(message)));
    }

    /** 造一轮最终回答 */
    public static ChatResponse answerResponse(String text) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
    }

    private static AssistantMessage newAssistantMessageWithToolCalls(List<AssistantMessage.ToolCall> toolCalls) {
        try {
            Constructor<AssistantMessage> ctor = AssistantMessage.class
                    .getDeclaredConstructor(String.class, Map.class, List.class, List.class);
            ctor.setAccessible(true);
            return ctor.newInstance("", Map.of(), toolCalls, List.<Media>of());
        } catch (Exception e) {
            throw new IllegalStateException("无法构造带 toolCalls 的 AssistantMessage", e);
        }
    }
}
