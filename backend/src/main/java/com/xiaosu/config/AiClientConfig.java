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
     * 注册模型可调用的工具。
     * 注意：不能用 Object[] 注入全部 bean——会与 Spring AI 的 toolCallbackResolver
     * 自动装配形成循环依赖（实测启动失败）。Task 16 起显式传入工具 bean。
     */
    @Bean
    public ToolCallbackProvider toolCallbackProvider() {
        return MethodToolCallbackProvider.builder().toolObjects().build();
    }
}
