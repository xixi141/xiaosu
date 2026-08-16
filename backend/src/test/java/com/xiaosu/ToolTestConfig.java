package com.xiaosu;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.util.List;

@TestConfiguration
public class ToolTestConfig {

    @Bean
    @Primary
    public ChatModel scriptedChatModel() {
        return new ScriptedChatModel(List.of(
                ScriptedChatModel.toolCallResponse("call-1", "order_stats",
                        "{\"from\":\"2026-08-10\",\"to\":\"2026-08-14\"}"),
                ScriptedChatModel.answerResponse("上周共 17 笔有效订单，销售总额为 166,500 元。")
        ));
    }

    @Bean
    @Primary
    public EmbeddingModel mockEmbeddingModel() {
        return new FakeAiModels.FakeEmbeddingModel();
    }
}
