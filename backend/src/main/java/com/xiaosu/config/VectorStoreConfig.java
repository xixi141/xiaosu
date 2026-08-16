package com.xiaosu.config;

import com.xiaosu.service.VectorStoreService;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class VectorStoreConfig {

    @Bean
    public VectorStoreService vectorStoreService(EmbeddingModel embeddingModel, AppProperties props) {
        // 构造时自动 load 持久化文件（若存在）
        return new VectorStoreService(embeddingModel, props);
    }
}
