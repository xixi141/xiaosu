package com.xiaosu.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import reactor.core.publisher.Flux;

/**
 * LLM 调用的重试边界：仅网络超时（ResourceAccessException）与 5xx 重试（1s/3s 退避）；
 * 4xx（401 坏 key、400 参数错）不重试，由 ChatService 直接降级（验收 7.5）。
 */
@Component
public class LlmGateway {

    @Retryable(retryFor = {ResourceAccessException.class, HttpServerErrorException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2))
    public Flux<ChatResponse> stream(ChatClient chatClient, Prompt prompt) {
        return chatClient.prompt(prompt)
                .stream()
                .chatResponse();
    }
}
