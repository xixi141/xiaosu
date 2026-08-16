package com.xiaosu.service;

import com.xiaosu.config.AppProperties;
import com.xiaosu.dto.DocumentDto;
import com.xiaosu.entity.DocumentChunkEntity;
import com.xiaosu.entity.DocumentEntity;
import com.xiaosu.repository.DocumentChunkRepository;
import com.xiaosu.repository.DocumentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Service
@Slf4j
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository chunkRepository;
    private final VectorStoreService vectorStoreService;
    private final AppProperties props;

    public DocumentService(DocumentRepository documentRepository,
                           DocumentChunkRepository chunkRepository,
                           VectorStoreService vectorStoreService,
                           AppProperties props) {
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.vectorStoreService = vectorStoreService;
        this.props = props;
    }

    public List<DocumentDto> list(int page, int size, String keyword) {
        var pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        var docs = (keyword == null || keyword.isBlank())
                ? documentRepository.findAll(pageable)
                : documentRepository.findByFilenameContainingIgnoreCase(keyword, pageable);
        return docs.stream().map(this::toDto).toList();
    }

    public DocumentDto detail(Long id) {
        DocumentEntity doc = documentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("文档不存在: " + id));
        List<DocumentDto.ChunkPreview> previews = chunkRepository
                .findByDocumentIdOrderByChunkIndex(id).stream()
                .map(c -> new DocumentDto.ChunkPreview(c.getChunkIndex(), previewOf(c.getContent()), c.getCharCount()))
                .toList();
        return new DocumentDto(doc.getId(), doc.getFilename(), doc.getFileType(), doc.getFileSize(),
                doc.getStatus().name(), doc.getChunkCount(), doc.getErrorMessage(), doc.getCreatedAt(), previews);
    }

    /** 删除文档：级联删切片行 + 精确删向量 + 删原件文件。保证「删除后不再命中」。 */
    @Transactional
    public void delete(Long id) {
        DocumentEntity doc = documentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("文档不存在: " + id));
        List<String> vectorIds = chunkRepository.findByDocumentIdOrderByChunkIndex(id).stream()
                .map(DocumentChunkEntity::getVectorId)
                .toList();
        if (!vectorIds.isEmpty()) {
            vectorStoreService.delete(vectorIds);
        }
        chunkRepository.deleteByDocumentId(id);
        documentRepository.delete(doc);
        // 原件按 {uploadDir}/{id}.{ext} 确定性命名（见 DocumentIngestService.saveOriginal）
        try {
            Files.deleteIfExists(Path.of(props.uploadDir(), id + "." + doc.getFileType()));
        } catch (IOException e) {
            log.warn("原件删除失败: {}.{}", id, doc.getFileType());
        }
        log.info("文档已删除: {} (切片 {} 个)", doc.getFilename(), vectorIds.size());
    }

    private DocumentDto toDto(DocumentEntity d) {
        return new DocumentDto(d.getId(), d.getFilename(), d.getFileType(), d.getFileSize(),
                d.getStatus().name(), d.getChunkCount(), d.getErrorMessage(), d.getCreatedAt(), List.of());
    }

    private String previewOf(String content) {
        return content.length() > 120 ? content.substring(0, 120) + "…" : content;
    }
}
