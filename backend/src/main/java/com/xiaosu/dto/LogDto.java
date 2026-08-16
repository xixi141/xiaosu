package com.xiaosu.dto;

import java.util.List;

public record LogDto(
        Long id,
        String sessionId,
        String userId,
        String question,
        String answer,
        String model,
        Integer totalTokens,
        Long latencyMs,
        String status,
        Boolean isRefused,
        String errorMessage,
        List<ChatResponseDto.ToolCallInfo> toolCalls,
        List<Citation> citations,
        String createdAt
) {
    public record Page(List<LogDto> items, long total) {
    }
}
