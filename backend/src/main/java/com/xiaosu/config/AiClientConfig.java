package com.xiaosu.config;

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
     * 收集所有 @Tool 注解方法，注册为模型可调用的工具。
     * 工具组件（EmployeeTool 等）就位后自动生效；Object[] 会扫全部 bean，
     * 只有带 @Tool 方法的类产生回调。
     */
    @Bean
    public ToolCallbackProvider toolCallbackProvider(Object[] toolObjects) {
        return MethodToolCallbackProvider.builder().toolObjects(toolObjects).build();
    }
}
