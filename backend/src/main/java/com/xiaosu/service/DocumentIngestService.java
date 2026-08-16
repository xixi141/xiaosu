package com.xiaosu.service;

import com.xiaosu.config.AppProperties;
import com.xiaosu.dto.IngestResult;
import com.xiaosu.entity.DocumentChunkEntity;
import com.xiaosu.entity.DocumentEntity;
import com.xiaosu.repository.DocumentChunkRepository;
import com.xiaosu.repository.DocumentRepository;
import com.xiaosu.util.Sha256Util;
import com.xiaosu.util.XiaosuTextSplitter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class DocumentIngestService {

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository chunkRepository;
    private final DocumentService documentService;
    private final VectorStoreService vectorStoreService;
    private final XiaosuTextSplitter splitter;
    private final AppProperties props;

    public DocumentIngestService(DocumentRepository documentRepository,
                                 DocumentChunkRepository chunkRepository,
                                 DocumentService documentService,
                                 VectorStoreService vectorStoreService,
                                 XiaosuTextSplitter splitter,
                                 AppProperties props) {
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.documentService = documentService;
        this.vectorStoreService = vectorStoreService;
        this.splitter = splitter;
        this.props = props;
    }

    /**
     * 入库全链路（增量更新分水岭）：
     * 1. 同内容（SHA256 命中）→ duplicate，不重复处理；overwrite=true 时强制重建
     * 2. 同名不同内容 → 自动替换旧版本（旧文档连同切片/向量/原件一并删除）
     * 3. 新文档 → Tika 解析 → 中文切块 → embedding → 向量库 → 落库
     */
    @Transactional
    public IngestResult ingest(byte[] bytes, String filename, boolean overwrite) {
        String sha256 = Sha256Util.hex(bytes);
        var sameContent = documentRepository.findBySha256(sha256);
        if (sameContent.isPresent()) {
            if (!overwrite) {
                log.info("文档内容未变化，跳过重复处理: {} ({})", filename, sha256);
                return IngestResult.duplicate(filename, sha256, sameContent.get().getId());
            }
            documentService.delete(sameContent.get().getId());
        } else {
            // 同名不同内容：替换旧版本，保证知识库里每个文件名只有一份最新版本
            List<DocumentEntity> sameName = documentRepository.findByFilenameOrderByCreatedAtDesc(filename);
            if (!sameName.isEmpty()) {
                for (DocumentEntity old : sameName) {
                    documentService.delete(old.getId());
                }
                log.info("同名文档替换: {}（旧版本 {} 份已删除）", filename, sameName.size());
            }
        }

        DocumentEntity doc = new DocumentEntity();
        doc.setFilename(filename);
        doc.setFileType(extensionOf(filename));
        doc.setFileSize((long) bytes.length);
        doc.setSha256(sha256);
        doc.setStatus(DocumentEntity.Status.PARSING);
        doc.setCreatedAt(LocalDateTime.now());
        doc.setUpdatedAt(LocalDateTime.now());
        documentRepository.save(doc);

        List<String> addedVectorIds = new ArrayList<>();
        try {
            saveOriginal(bytes, doc);
            List<String> chunks = parseAndSplit(bytes, filename);
            if (chunks.isEmpty()) {
                throw new IllegalStateException("文档解析后无有效内容");
            }

            List<Document> vectors = new ArrayList<>();
            List<DocumentChunkEntity> chunkRows = new ArrayList<>();
            for (int i = 0; i < chunks.size(); i++) {
                String vectorId = "doc" + doc.getId() + "c" + i;
                vectors.add(Document.builder()
                        .id(vectorId)
                        .text(chunks.get(i))
                        .metadata(Map.of(
                                "documentId", String.valueOf(doc.getId()),
                                "filename", filename,
                                "chunkIndex", i))
                        .build());
                DocumentChunkEntity row = new DocumentChunkEntity();
                row.setDocumentId(doc.getId());
                row.setVectorId(vectorId);
                row.setChunkIndex(i);
                row.setContent(chunks.get(i));
                row.setCharCount(chunks.get(i).length());
                row.setCreatedAt(LocalDateTime.now());
                chunkRows.add(row);
            }

            vectorStoreService.add(vectors);
            addedVectorIds.addAll(vectors.stream().map(Document::getId).toList());
            chunkRepository.saveAll(chunkRows);

            doc.setStatus(DocumentEntity.Status.READY);
            doc.setChunkCount(chunks.size());
            doc.setUpdatedAt(LocalDateTime.now());
            documentRepository.save(doc);

            log.info("文档入库完成: {} 切片 {} 个", filename, chunks.size());
            return new IngestResult(doc.getId(), filename, sha256, "READY", chunks.size(), false, null);
        } catch (Exception e) {
            log.error("文档入库失败: {}", filename, e);
            // 向量库不参与 DB 事务：失败时回滚已写入的向量，保证「FAILED 文档不可检索」
            if (!addedVectorIds.isEmpty()) {
                try {
                    vectorStoreService.delete(addedVectorIds);
                } catch (Exception ve) {
                    log.error("失败清理向量异常: {}", filename, ve);
                }
            }
            deleteOriginalQuietly(doc);
            doc.setStatus(DocumentEntity.Status.FAILED);
            doc.setErrorMessage(e.getMessage());
            doc.setUpdatedAt(LocalDateTime.now());
            documentRepository.save(doc);
            return IngestResult.failed(filename, sha256, e.getMessage());
        }
    }

    public void delete(Long id) {
        documentService.delete(id);
    }

    private List<String> parseAndSplit(byte[] bytes, String filename) {
        TikaDocumentReader reader = new TikaDocumentReader(new ByteArrayResource(bytes));
        List<Document> parsed = reader.get();
        if (parsed.isEmpty()) {
            throw new IllegalStateException("Tika 未解析出内容: " + filename);
        }
        List<String> chunks = new ArrayList<>();
        for (Document d : parsed) {
            chunks.addAll(splitter.split(d.getText(), props.rag().chunkSize(), props.rag().chunkOverlap()));
        }
        return chunks;
    }

    /** 原件按 {uploadDir}/{documentId}.{ext} 确定性命名，删除时可精确清理。 */
    private void saveOriginal(byte[] bytes, DocumentEntity doc) throws IOException {
        Path dir = Path.of(props.uploadDir());
        Files.createDirectories(dir);
        Files.write(dir.resolve(doc.getId() + "." + doc.getFileType()), bytes);
    }

    private void deleteOriginalQuietly(DocumentEntity doc) {
        try {
            Files.deleteIfExists(Path.of(props.uploadDir(), doc.getId() + "." + doc.getFileType()));
        } catch (IOException e) {
            log.warn("原件清理失败: {}.{}", doc.getId(), doc.getFileType());
        }
    }

    private String extensionOf(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot < 0 ? "bin" : filename.substring(dot + 1).toLowerCase();
    }
}
