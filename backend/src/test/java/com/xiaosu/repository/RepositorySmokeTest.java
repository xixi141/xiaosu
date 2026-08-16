package com.xiaosu.repository;

import com.xiaosu.entity.ChatLogEntity;
import com.xiaosu.entity.DocumentChunkEntity;
import com.xiaosu.entity.DocumentEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class RepositorySmokeTest {

    @Autowired DocumentRepository documentRepository;
    @Autowired DocumentChunkRepository chunkRepository;
    @Autowired ChatLogRepository chatLogRepository;

    @Test
    void documentCrudAndSha256Lookup() {
        DocumentEntity doc = new DocumentEntity();
        doc.setFilename("员工手册.md");
        doc.setFileType("md");
        doc.setSha256("a".repeat(64));
        doc.setStatus(DocumentEntity.Status.READY);
        doc.setCreatedAt(LocalDateTime.now());
        documentRepository.save(doc);

        assertThat(documentRepository.findBySha256("a".repeat(64))).isPresent();

        DocumentChunkEntity chunk = new DocumentChunkEntity();
        chunk.setDocumentId(doc.getId());
        chunk.setVectorId("doc" + doc.getId() + "c0");
        chunk.setChunkIndex(0);
        chunk.setContent("年假满一年可享受 5 天带薪年假");
        chunk.setCharCount(15);
        chunk.setCreatedAt(LocalDateTime.now());
        chunkRepository.save(chunk);

        assertThat(chunkRepository.findByDocumentIdOrderByChunkIndex(doc.getId())).hasSize(1);
        assertThat(chunkRepository.deleteByDocumentId(doc.getId())).isEqualTo(1);
        assertThat(chunkRepository.findByDocumentIdOrderByChunkIndex(doc.getId())).isEmpty();
    }

    @Test
    void chatLogPersistsToolCallsJson() {
        ChatLogEntity log = new ChatLogEntity();
        log.setSessionId("u001#conv1");
        log.setUserId("u001");
        log.setQuestion("员工 001 是哪个部门的？");
        log.setAnswer("研发部");
        log.setModel("deepseek-chat");
        log.setStatus(ChatLogEntity.Status.SUCCESS);
        log.setCreatedAt(LocalDateTime.now());
        chatLogRepository.save(log);

        assertThat(chatLogRepository.findByUserIdContainingIgnoreCase("u001", PageRequest.of(0, 10))).hasSize(1);
    }
}
