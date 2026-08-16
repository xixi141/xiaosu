package com.xiaosu.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.xiaosu.dto.ChatResponseDto.ToolCallInfo;
import com.xiaosu.dto.Citation;
import com.xiaosu.dto.LogDto;
import com.xiaosu.entity.ChatLogEntity;
import com.xiaosu.repository.ChatLogRepository;
import com.xiaosu.util.JsonUtil;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LogService {

    private static final TypeReference<List<ToolCallInfo>> TOOL_CALLS_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<List<Citation>> CITATIONS_TYPE = new TypeReference<>() {
    };

    private final ChatLogRepository chatLogRepository;

    public LogService(ChatLogRepository chatLogRepository) {
        this.chatLogRepository = chatLogRepository;
    }

    public LogDto.Page list(int page, int size, String userId, String status) {
        var pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        boolean hasUser = userId != null && !userId.isBlank();
        boolean hasStatus = status != null && !status.isBlank();
        var result = hasUser && hasStatus
                ? chatLogRepository.findByUserIdContainingIgnoreCaseAndStatus(userId, ChatLogEntity.Status.valueOf(status), pageable)
                : hasUser
                ? chatLogRepository.findByUserIdContainingIgnoreCase(userId, pageable)
                : hasStatus
                ? chatLogRepository.findByStatus(ChatLogEntity.Status.valueOf(status), pageable)
                : chatLogRepository.findAll(pageable);
        return new LogDto.Page(result.getContent().stream().map(this::toDto).toList(), result.getTotalElements());
    }

    public LogDto detail(Long id) {
        return chatLogRepository.findById(id).map(this::toDto)
                .orElseThrow(() -> new IllegalArgumentException("日志不存在: " + id));
    }

    private LogDto toDto(ChatLogEntity e) {
        List<ToolCallInfo> toolCalls = JsonUtil.fromJson(e.getToolCalls(), TOOL_CALLS_TYPE);
        List<Citation> citations = JsonUtil.fromJson(e.getCitations(), CITATIONS_TYPE);
        return new LogDto(
                e.getId(), e.getSessionId(), e.getUserId(), e.getQuestion(), e.getAnswer(),
                e.getModel(), e.getTotalTokens(), e.getLatencyMs(), e.getStatus().name(),
                e.getIsRefused(), e.getErrorMessage(),
                toolCalls == null ? List.of() : toolCalls,
                citations == null ? List.of() : citations,
                e.getCreatedAt() == null ? null : e.getCreatedAt().toString());
    }
}
