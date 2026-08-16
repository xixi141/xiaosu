package com.xiaosu.memory;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;

import java.util.List;

/**
 * 只保留「干净对话」的记忆：存入 user 消息与不含工具调用的 assistant 回答，
 * 丢弃 tool 消息与带 tool_calls 的 assistant 消息。
 *
 * 原因（实测坑 #4）：工具消息进入 MessageWindowChatMemory 后，窗口截断可能把
 * tool 消息与它前面的 tool_calls 消息拆散，DeepSeek 会拒绝请求
 * （"Messages with role 'tool' must be a response to a preceding message with 'tool_calls'"）。
 * 工具调用的完整轨迹已由 chat_log.tool_calls 落库，记忆里不需要它。
 */
public class FilteredChatMemory implements ChatMemory {

    private final MessageWindowChatMemory delegate;

    public FilteredChatMemory(int windowSize) {
        this.delegate = MessageWindowChatMemory.builder().maxMessages(windowSize).build();
    }

    @Override
    public void add(String conversationId, List<Message> messages) {
        List<Message> filtered = messages.stream()
                .filter(m -> m.getMessageType() == MessageType.USER
                        || (m.getMessageType() == MessageType.ASSISTANT
                            && m instanceof AssistantMessage am && !am.hasToolCalls()))
                .toList();
        if (!filtered.isEmpty()) {
            delegate.add(conversationId, filtered);
        }
    }

    @Override
    public List<Message> get(String conversationId) {
        return delegate.get(conversationId);
    }

    @Override
    public void clear(String conversationId) {
        delegate.clear(conversationId);
    }
}
