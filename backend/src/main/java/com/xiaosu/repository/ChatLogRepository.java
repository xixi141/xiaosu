package com.xiaosu.repository;

import com.xiaosu.entity.ChatLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatLogRepository extends JpaRepository<ChatLogEntity, Long> {
    Page<ChatLogEntity> findAll(Pageable pageable);

    Page<ChatLogEntity> findByStatus(ChatLogEntity.Status status, Pageable pageable);

    Page<ChatLogEntity> findByUserIdContainingIgnoreCase(String userId, Pageable pageable);

    Page<ChatLogEntity> findByUserIdContainingIgnoreCaseAndStatus(String userId, ChatLogEntity.Status status, Pageable pageable);
}
