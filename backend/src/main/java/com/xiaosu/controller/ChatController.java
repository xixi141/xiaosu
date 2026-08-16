package com.xiaosu.controller;

import com.xiaosu.dto.ChatRequest;
import com.xiaosu.dto.ChatResponseDto;
import com.xiaosu.service.ChatService;
import com.xiaosu.util.JsonUtil;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public ChatResponseDto ask(@Valid @RequestBody ChatRequest req) {
        return chatService.ask(req);
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> stream(@Valid @RequestBody ChatRequest req) {
        return chatService.stream(req)
                .map(ev -> ServerSentEvent.builder(JsonUtil.toJson(ev))
                        .event(ev.type())
                        .build());
    }
}
