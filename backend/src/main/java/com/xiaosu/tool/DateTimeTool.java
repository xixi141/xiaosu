package com.xiaosu.tool;

import com.xiaosu.util.JsonUtil;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Component
public class DateTimeTool {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Tool(name = "current_time", description = "获取当前日期和时间（yyyy-MM-dd HH:mm:ss，含星期）。计算「上周/本月」等相对日期前先调用本工具")
    public String now(ToolContext toolContext) {
        LocalDateTime now = LocalDateTime.now();
        String result = now.format(FMT) + "（" + dayOfWeek(now.getDayOfWeek().getValue()) + "）";
        ToolRecorder.record(toolContext, "current_time", "{}", result);
        return result;
    }

    private String dayOfWeek(int value) {
        return switch (value) {
            case 1 -> "周一";
            case 2 -> "周二";
            case 3 -> "周三";
            case 4 -> "周四";
            case 5 -> "周五";
            case 6 -> "周六";
            default -> "周日";
        };
    }
}
