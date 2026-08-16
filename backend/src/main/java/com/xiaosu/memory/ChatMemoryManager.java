package com.xiaosu.memory;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 按 sessionKey（userId#conversationId）隔离的多轮记忆：
 * 每个会话一个 FilteredChatMemory（最近 20 条干净对话），
 * A 的上下文不可能被 B 接到（验收 7.3）。
 *
 * 注意：不使用 MessageChatMemoryAdvisor——它在工具循环的第二次内部模型调用中
 * 会重排消息列表、吞掉 assistant tool_calls 消息（实测坑 #4），
 * 改为 ChatService 手动拼接历史消息。
 */
@Component
@Slf4j
public class ChatMemoryManager {

    public static final int WINDOW_SIZE = 20;

    private final Map<String, FilteredChatMemory> memories = new ConcurrentHashMap<>();
    private final Map<String, Long> lastAccess = new ConcurrentHashMap<>();

    public List<Message> historyFor(String sessionKey) {
        FilteredChatMemory memory = memories.computeIfAbsent(sessionKey, k -> new FilteredChatMemory(WINDOW_SIZE));
        lastAccess.put(sessionKey, System.currentTimeMillis());
        return memory.get(sessionKey);
    }

    /** 保存一轮干净对话（user 提问 + 不含工具调用的 assistant 回答） */
    public void save(String sessionKey, Message userMessage, Message assistantMessage) {
        FilteredChatMemory memory = memories.computeIfAbsent(sessionKey, k -> new FilteredChatMemory(WINDOW_SIZE));
        memory.add(sessionKey, List.of(userMessage, assistantMessage));
        lastAccess.put(sessionKey, System.currentTimeMillis());
    }

    /** 清理长时间未访问的会话（防止内存无限增长） */
    public void evictExpired(long ttlMillis) {
        long now = System.currentTimeMillis();
        lastAccess.forEach((key, time) -> {
            if (now - time > ttlMillis) {
                memories.remove(key);
                lastAccess.remove(key);
                log.info("会话记忆已过期清理: {}", key);
            }
        });
    }

    public int activeSessions() {
        return memories.size();
    }
}
