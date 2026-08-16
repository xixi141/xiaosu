package com.xiaosu;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * 测试专用配置：FakeChatModel/FakeEmbeddingModel 替换真实模型，全程不触网。
 * （Spring AI 1.1.8 不含官方 Mock 模型，见 FakeAiModels 说明）
 */
@TestConfiguration
public class TestAiConfig {

    @Bean
    @Primary
    public ChatModel mockChatModel() {
        return new FakeAiModels.FakeChatModel("这是模拟回答[1]");
    }

    @Bean
    @Primary
    public EmbeddingModel mockEmbeddingModel() {
        return new FakeAiModels.FakeEmbeddingModel();
    }
}
