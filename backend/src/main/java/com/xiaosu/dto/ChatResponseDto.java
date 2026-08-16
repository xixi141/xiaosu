package com.xiaosu.dto;

import org.springframework.ai.chat.metadata.Usage;

import java.util.List;

public record ChatResponseDto(
        String answer,
        List<Citation> citations,
        List<ToolCallInfo> toolCalls,
        UsageInfo usage,
        String status
) {
    public static ChatResponseDto refused(String answer) {
        return new ChatResponseDto(answer, List.of(), List.of(), new UsageInfo(0, 0, 0), "REFUSED");
    }

    public static ChatResponseDto fallback(String answer) {
        return new ChatResponseDto(answer, List.of(), List.of(), new UsageInfo(0, 0, 0), "FALLBACK");
    }

    /** 一次工具调用的记录 */
    public record ToolCallInfo(String name, String arguments, String resultSummary) {
    }

    /** token 用量（对应验收 7.6「Token 消耗」展示） */
    public record UsageInfo(Integer inputTokens, Integer outputTokens, Integer totalTokens) {
        public static UsageInfo from(Usage usage) {
            if (usage == null) {
                return new UsageInfo(0, 0, 0);
            }
            return new UsageInfo(safe(usage.getPromptTokens()), safe(usage.getCompletionTokens()), safe(usage.getTotalTokens()));
        }

        private static Integer safe(Integer v) {
            return v == null ? 0 : v;
        }
    }
}
