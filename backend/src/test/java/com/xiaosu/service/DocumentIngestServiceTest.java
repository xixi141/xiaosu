package com.xiaosu.service;

import com.xiaosu.TestAiConfig;
import com.xiaosu.config.AppProperties;
import com.xiaosu.dto.IngestResult;
import com.xiaosu.entity.DocumentEntity;
import com.xiaosu.repository.DocumentChunkRepository;
import com.xiaosu.repository.DocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

// FakeEmbeddingModel 的相似度分布与真实模型不同（稀疏字符桶向量），
// 测试将阈值置 0 让相似度过滤不干扰检索断言
@SpringBootTest(properties = "xiaosu.rag.similarity-threshold=0.0")
@Import(TestAiConfig.class)
class DocumentIngestServiceTest {

    @Autowired DocumentIngestService ingestService;
    @Autowired DocumentRepository documentRepository;
    @Autowired DocumentChunkRepository chunkRepository;
    @Autowired VectorStoreService vectorStoreService;
    @Autowired AppProperties props;

    @TempDir Path tempDir;

    private static final String MD = """
            # 测试手册

            ## 年假
            员工工作满一年后每年可享受 5 天带薪年假，需提前 3 个工作日申请。

            ## 报销
            报销需提供增值税发票原件、费用明细清单，金额超过 2000 元需附支付凭证。
            """;

    @BeforeEach
    void setUp() {
        documentRepository.deleteAll();
        chunkRepository.deleteAll();
        vectorStoreService.reset(tempDir.resolve("vs.json").toString());
    }

    private IngestResult ingest(String filename) {
        return ingestService.ingest(MD.getBytes(StandardCharsets.UTF_8), filename, false);
    }

    @Test
    void ingestsDocumentWithChunksAndVectors() {
        IngestResult result = ingest("测试手册.md");

        assertThat(result.status()).isEqualTo("READY");
        assertThat(result.chunkCount()).isGreaterThan(0);
        DocumentEntity doc = documentRepository.findBySha256(result.sha256()).orElseThrow();
        assertThat(doc.getStatus()).isEqualTo(DocumentEntity.Status.READY);
        assertThat(chunkRepository.findByDocumentIdOrderByChunkIndex(doc.getId())).hasSize(result.chunkCount());
        assertThat(vectorStoreService.count()).isEqualTo(result.chunkCount());
        assertThat(vectorStoreService.search("年假几天").get(0).filename()).isEqualTo("测试手册.md");
    }

    @Test
    void sameContentReturnsDuplicate() {
        ingest("测试手册.md");
        IngestResult again = ingest("测试手册.md");

        assertThat(again.duplicate()).isTrue();
        assertThat(documentRepository.count()).isEqualTo(1);
    }

    @Test
    void overwriteReplacesOldChunks() {
        IngestResult first = ingest("测试手册.md");
        String newContent = MD + "\n## 新增章节\n新内容：仅此一句，覆盖后应能检索到。";
        IngestResult second = ingestService.ingest(newContent.getBytes(StandardCharsets.UTF_8), "测试手册.md", true);

        assertThat(second.duplicate()).isFalse();
        assertThat(documentRepository.count()).isEqualTo(1);
        assertThat(chunkRepository.findByDocumentIdOrderByChunkIndex(first.documentId())).isEmpty();
        assertThat(vectorStoreService.search("新增章节").get(0).content()).contains("覆盖后应能检索到");
    }

    @Test
    void deleteRemovesChunksVectorsAndFile() {
        IngestResult result = ingest("测试手册.md");
        Long docId = result.documentId();

        ingestService.delete(docId);

        assertThat(documentRepository.findById(docId)).isEmpty();
        assertThat(chunkRepository.findByDocumentIdOrderByChunkIndex(docId)).isEmpty();
        assertThat(vectorStoreService.search("年假")).isEmpty();
    }
}
