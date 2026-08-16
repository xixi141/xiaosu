package com.xiaosu.repository;

import com.xiaosu.entity.DocumentChunkEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentChunkRepository extends JpaRepository<DocumentChunkEntity, Long> {
    List<DocumentChunkEntity> findByDocumentIdOrderByChunkIndex(Long documentId);

    List<DocumentChunkEntity> findByDocumentIdIn(List<Long> documentIds);

    long deleteByDocumentId(Long documentId);
}
