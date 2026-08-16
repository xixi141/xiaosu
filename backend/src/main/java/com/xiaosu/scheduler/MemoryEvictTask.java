package com.xiaosu.scheduler;

import com.xiaosu.memory.ChatMemoryManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class MemoryEvictTask {

    private static final long TTL_MILLIS = 30 * 60 * 1000L;

    private final ChatMemoryManager memoryManager;

    public MemoryEvictTask(ChatMemoryManager memoryManager) {
        this.memoryManager = memoryManager;
    }

    @Scheduled(fixedRate = 10 * 60 * 1000L)
    public void evict() {
        memoryManager.evictExpired(TTL_MILLIS);
    }
}
