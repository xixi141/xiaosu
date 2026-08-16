package com.xiaosu;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 真实 API 连通性冒烟：需要 .env 中的真实 key，仅手动跑。
 * 运行：set -a && source .env && set +a && mvn test -Dtest=AiConnectivityLiveTest
 * （mvn test 默认跑全部测试类，此测试无 @Tag 排除需手动指定 -Dtest）
 */
@SpringBootTest
@Tag("live")
class AiConnectivityLiveTest {

    @Autowired ChatModel chatModel;
    @Autowired EmbeddingModel embeddingModel;

    @Test
    void deepseekChatWorks() {
        ChatResponse response = chatModel.call(new Prompt("用一句话回答：1+1 等于几？"));
        String text = response.getResult().getOutput().getText();
        System.out.println("CHAT 回复: " + text);
        assertThat(text).contains("2");
    }

    @Test
    void dashscopeEmbeddingWorks() {
        EmbeddingResponse response = embeddingModel.call(new EmbeddingRequest(List.of("员工年假制度"), null));
        System.out.println("EMBEDDING 维度: " + response.getResults().get(0).getOutput().length);
        assertThat(response.getResults().get(0).getOutput()).hasSize(1024);
    }
}
