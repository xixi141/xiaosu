package com.xiaosu.dto;

import java.util.Map;

public record HealthDto(
        String status,
        String db,
        long vectorStoreCount,
        String chatModel,
        String embeddingModel,
        Map<String, Object> dingtalk,
        String time
) {
}
