package com.xiaosu.controller;

import com.xiaosu.config.AppProperties;
import com.xiaosu.dto.HealthDto;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final DataSource dataSource;
    private final AppProperties props;
    private final ChatModel chatModel;
    private final EmbeddingModel embeddingModel;

    public HealthController(DataSource dataSource, AppProperties props,
                            ChatModel chatModel, EmbeddingModel embeddingModel) {
        this.dataSource = dataSource;
        this.props = props;
        this.chatModel = chatModel;
        this.embeddingModel = embeddingModel;
    }

    @GetMapping
    public HealthDto health() {
        boolean dbOk = true;
        try (Connection ignored = dataSource.getConnection()) {
            // 连接成功即视为 DB UP
        } catch (Exception e) {
            dbOk = false;
        }
        return new HealthDto(
                dbOk ? "UP" : "DEGRADED",
                dbOk ? "UP" : "DOWN",
                0,
                chatModel.getClass().getSimpleName(),
                embeddingModel.getClass().getSimpleName(),
                Map.of(
                        "enabled", props.dingtalk().enabled(),
                        "connected", false
                ),
                LocalDateTime.now().toString()
        );
    }
}
