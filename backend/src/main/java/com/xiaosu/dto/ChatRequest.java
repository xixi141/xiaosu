package com.xiaosu.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatRequest(
        @NotBlank String sessionId,
        String userId,
        @NotBlank String question
) {
}
