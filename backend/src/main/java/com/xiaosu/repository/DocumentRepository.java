package com.xiaosu.repository;

import com.xiaosu.entity.DocumentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentRepository extends JpaRepository<DocumentEntity, Long> {
    Optional<DocumentEntity> findBySha256(String sha256);

    List<DocumentEntity> findByFilenameOrderByCreatedAtDesc(String filename);

    Page<DocumentEntity> findByFilenameContainingIgnoreCase(String keyword, Pageable pageable);
}
