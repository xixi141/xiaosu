package com.xiaosu.tool;

import com.xiaosu.dto.ChatResponseDto.ToolCallInfo;
import org.springframework.ai.chat.model.ToolContext;

import java.util.ArrayList;
import java.util.List;

/**
 * 一次问答内工具调用的记录器。ChatService 创建后放入 toolContext（Map），
 * 工具方法通过 ToolContext 参数取出并记录（跨线程安全，不依赖 ThreadLocal）。
 */
public class ToolRecorder {

    public static final String TOOL_CONTEXT_KEY = "xiaosu.toolRecorder";

    private final List<ToolCallInfo> calls = new ArrayList<>();

    public synchronized void record(String name, String arguments, String resultSummary) {
        calls.add(new ToolCallInfo(name, arguments, resultSummary));
    }

    public synchronized List<ToolCallInfo> calls() {
        return List.copyOf(calls);
    }

    /** 工具方法内调用：从 ToolContext 取出 recorder 并记录（recorder 为空时静默跳过） */
    public static void record(ToolContext toolContext, String name, String arguments, String resultSummary) {
        if (toolContext == null || toolContext.getContext() == null) {
            return;
        }
        Object recorder = toolContext.getContext().get(TOOL_CONTEXT_KEY);
        if (recorder instanceof ToolRecorder r) {
            r.record(name, arguments, resultSummary);
        }
    }
}
