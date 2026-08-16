package com.xiaosu.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "xiaosu")
public record AppProperties(
        String uploadDir,
        String vectorStorePath,
        Rag rag,
        Dingtalk dingtalk
) {
    public record Rag(int topK, double similarityThreshold, int chunkSize, int chunkOverlap) {
    }

    public record Dingtalk(boolean enabled, String clientId, String clientSecret) {
    }

    /** 当前生效的 chat 模型名（配置全外置：来自 CHAT_MODEL 环境变量） */
    public String modelNameOrDefault() {
        String v = System.getenv("CHAT_MODEL");
        return (v == null || v.isBlank()) ? "deepseek-chat" : v;
    }
}
