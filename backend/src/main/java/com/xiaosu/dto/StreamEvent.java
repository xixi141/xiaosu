package com.xiaosu.dto;

import java.util.List;

/**
 * SSE 事件：{type:meta,citations[]} → {type:token,delta}×N → {type:done,usage,status}
 * 或 {type:error,message}。单 record + 静态工厂，type 字段判别。
 */
public record StreamEvent(
        String type,
        String delta,
        List<Citation> citations,
        List<ChatResponseDto.ToolCallInfo> toolCalls,
        ChatResponseDto.UsageInfo usage,
        String status,
        String message
) {
    public static StreamEvent meta(List<Citation> citations) {
        return new StreamEvent("meta", null, citations, List.of(), null, null, null);
    }

    public static StreamEvent token(String delta) {
        return new StreamEvent("token", delta, null, null, null, null, null);
    }

    public static StreamEvent done(ChatResponseDto.UsageInfo usage, List<ChatResponseDto.ToolCallInfo> toolCalls, String status) {
        return new StreamEvent("done", null, null, toolCalls, usage, status, null);
    }

    public static StreamEvent error(String message) {
        return new StreamEvent("error", null, null, null, null, "FALLBACK", message);
    }
}
