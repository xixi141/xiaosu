package com.xiaosu.service;

import com.xiaosu.config.AppProperties;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class SettingService {

    private final AppProperties props;
    private final EmbeddingModel embeddingModel;

    public SettingService(AppProperties props, EmbeddingModel embeddingModel) {
        this.props = props;
        this.embeddingModel = embeddingModel;
    }

    public Map<String, Object> settings() {
        // 超过 10 对键值，Map.of 不支持，用 LinkedHashMap 保持顺序
        Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("chatModel", env("CHAT_MODEL", "deepseek-chat"));
        map.put("embeddingModel", env("EMBEDDING_MODEL", "text-embedding-v4"));
        map.put("chatBaseUrl", env("OPENAI_BASE_URL", "https://api.deepseek.com"));
        map.put("embeddingBaseUrl", env("EMBEDDING_BASE_URL", "https://dashscope.aliyuncs.com/compatible-mode/v1"));
        map.put("chatApiKeyMasked", mask(env("DEEPSEEK_API_KEY", "")));
        map.put("embeddingApiKeyMasked", mask(env("DASHSCOPE_API_KEY", "")));
        map.put("topK", props.rag().topK());
        map.put("threshold", props.rag().similarityThreshold());
        map.put("chunkSize", props.rag().chunkSize());
        map.put("chunkOverlap", props.rag().chunkOverlap());
        map.put("dingtalkEnabled", props.dingtalk().enabled());
        return map;
    }

    /** 连通测试：实际调一次 embedding 服务，验证 key 有效性（演示 7.5 时先在设置页发现异常） */
    public Map<String, Object> testConnection() {
        long start = System.currentTimeMillis();
        try {
            var resp = embeddingModel.call(new EmbeddingRequest(List.of("连通性测试"), null));
            long latency = System.currentTimeMillis() - start;
            return Map.of("ok", true,
                    "message", "Embedding 服务连通，返回维度 " + resp.getResults().get(0).getOutput().length,
                    "latencyMs", latency);
        } catch (Exception e) {
            return Map.of("ok", false, "message", "连接失败: " + e.getMessage(),
                    "latencyMs", System.currentTimeMillis() - start);
        }
    }

    private String env(String key, String defaultValue) {
        String v = System.getenv(key);
        return (v == null || v.isBlank()) ? defaultValue : v;
    }

    private String mask(String key) {
        if (key == null || key.isBlank()) {
            return "（未配置）";
        }
        if (key.length() <= 4) {
            return "****";
        }
        return "****" + key.substring(key.length() - 4);
    }
}
