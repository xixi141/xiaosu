package com.xiaosu.memory;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 按 sessionKey（userId#conversationId）隔离的多轮记忆：
 * 每个会话一个 MessageWindowChatMemory（最近 10 条），A 的上下文不可能被 B 接到（验收 7.3）。
 */
@Component
@Slf4j
public class ChatMemoryManager {

    public static final int WINDOW_SIZE = 10;

    private final Map<String, MessageWindowChatMemory> memories = new ConcurrentHashMap<>();
    private final Map<String, Long> lastAccess = new ConcurrentHashMap<>();

    public MessageChatMemoryAdvisor advisorFor(String sessionKey) {
        memories.computeIfAbsent(sessionKey, k -> MessageWindowChatMemory.builder()
                .maxMessages(WINDOW_SIZE)
                .build());
        lastAccess.put(sessionKey, System.currentTimeMillis());
        return MessageChatMemoryAdvisor.builder(memories.get(sessionKey)).build();
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
