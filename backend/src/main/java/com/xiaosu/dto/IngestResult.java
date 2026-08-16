package com.xiaosu.dto;

public record IngestResult(
        Long documentId,
        String filename,
        String sha256,
        String status,
        int chunkCount,
        boolean duplicate,
        String errorMessage
) {
    public static IngestResult duplicate(String filename, String sha256, Long documentId) {
        return new IngestResult(documentId, filename, sha256, "READY", 0, true, null);
    }

    public static IngestResult failed(String filename, String sha256, String errorMessage) {
        return new IngestResult(null, filename, sha256, "FAILED", 0, false, errorMessage);
    }
}
