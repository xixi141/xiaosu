package com.xiaosu;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

/**
 * 自写 Fake 模型（Spring AI 1.1.8 无官方 Mock 模型，实测本地仓库所有构件均不含
 * MockChatModel/MockEmbeddingModel）。全程不触网，保证测试确定性。
 *
 * FakeEmbeddingModel 用字符桶哈希生成伪向量（256 维）：
 * 含相同字符的文本共享桶位 → 余弦相似度更高 → 检索命中有语义相关的文本。
 */
final class FakeAiModels {

    static final int EMBEDDING_DIM = 256;

    private FakeAiModels() {
    }

    static class FakeChatModel implements ChatModel {
        private final String fixedAnswer;

        FakeChatModel(String fixedAnswer) {
            this.fixedAnswer = fixedAnswer;
        }

        @Override
        public ChatResponse call(Prompt prompt) {
            return response(fixedAnswer);
        }

        @Override
        public Flux<ChatResponse> stream(Prompt prompt) {
            return Flux.just(response(fixedAnswer));
        }

        @Override
        public ChatOptions getDefaultOptions() {
            return ChatOptions.builder().build();
        }

        private ChatResponse response(String text) {
            return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
        }
    }

    static class FakeEmbeddingModel implements EmbeddingModel {

        @Override
        public EmbeddingResponse call(EmbeddingRequest request) {
            List<Embedding> embeddings = new ArrayList<>();
            for (int i = 0; i < request.getInstructions().size(); i++) {
                embeddings.add(new Embedding(charBucketVector(request.getInstructions().get(i)), i));
            }
            return new EmbeddingResponse(embeddings);
        }

        @Override
        public float[] embed(Document document) {
            return charBucketVector(getEmbeddingContent(document));
        }
    }

    private static float[] charBucketVector(String text) {
        float[] vector = new float[EMBEDDING_DIM];
        for (char c : text.toCharArray()) {
            int bucket = Math.floorMod(c, EMBEDDING_DIM);
            vector[bucket] += 1.0f;
        }
        normalize(vector);
        return vector;
    }

    private static void normalize(float[] vector) {
        double norm = 0;
        for (float v : vector) {
            norm += v * v;
        }
        if (norm == 0) {
            return;
        }
        float length = (float) Math.sqrt(norm);
        for (int i = 0; i < vector.length; i++) {
            vector[i] /= length;
        }
    }
}
