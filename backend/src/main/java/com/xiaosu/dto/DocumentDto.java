package com.xiaosu.dto;

import java.time.LocalDateTime;
import java.util.List;

public record DocumentDto(
        Long id,
        String filename,
        String fileType,
        Long fileSize,
        String status,
        Integer chunkCount,
        String errorMessage,
        LocalDateTime createdAt,
        List<ChunkPreview> chunks
) {
    public record ChunkPreview(int index, String preview, int charCount) {
    }
}
