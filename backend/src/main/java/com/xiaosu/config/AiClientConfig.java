package com.xiaosu.config;

import com.xiaosu.tool.AttendanceTool;
import com.xiaosu.tool.DateTimeTool;
import com.xiaosu.tool.EmployeeTool;
import com.xiaosu.tool.OrderTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiClientConfig {

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder.build();
    }

    /**
     * 注册模型可调用的工具（显式列出，避免与 toolCallbackResolver 循环依赖）。
     */
    @Bean
    public ToolCallbackProvider toolCallbackProvider(EmployeeTool employeeTool,
                                                     AttendanceTool attendanceTool,
                                                     OrderTool orderTool,
                                                     DateTimeTool dateTimeTool) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(employeeTool, attendanceTool, orderTool, dateTimeTool)
                .build();
    }
}
