package com.xiaosu.service;

import com.xiaosu.TestAiConfig;
import com.xiaosu.rag.RagContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestAiConfig.class)
class VectorStoreServiceTest {

    @Autowired VectorStoreService vectorStoreService;

    @TempDir Path tempDir;

    @BeforeEach
    void setUp() {
        vectorStoreService.reset(tempDir.resolve("vector-store.json").toString());
    }

    @Test
    void addSearchAndDelete() {
        vectorStoreService.add(List.of(
                Document.builder().id("doc1c0").text("员工工作满一年后每年可享受 5 天带薪年假")
                        .metadata(Map.of("filename", "员工手册.md", "documentId", "1", "chunkIndex", 0)).build(),
                Document.builder().id("doc1c1").text("报销需提供增值税发票原件和费用明细清单")
                        .metadata(Map.of("filename", "员工手册.md", "documentId", "1", "chunkIndex", 1)).build()
        ));
        vectorStoreService.persist();

        assertThat(vectorStoreService.count()).isEqualTo(2);

        List<RagContext.RagHit> hits = vectorStoreService.search("年假有几天");
        assertThat(hits).isNotEmpty();
        assertThat(hits.get(0).filename()).isEqualTo("员工手册.md");
        assertThat(hits.get(0).content()).contains("年假");

        vectorStoreService.delete(List.of("doc1c0", "doc1c1"));
        assertThat(vectorStoreService.count()).isZero();
        assertThat(vectorStoreService.search("年假有几天")).isEmpty();
    }

    @Test
    void persistsAcrossReload() {
        vectorStoreService.add(List.of(
                Document.builder().id("doc2c0").text("出差住宿标准一线城市 500 元每晚上限")
                        .metadata(Map.of("filename", "出差管理制度.docx", "documentId", "2", "chunkIndex", 0)).build()
        ));
        vectorStoreService.persist();

        vectorStoreService.reload();

        assertThat(vectorStoreService.count()).isEqualTo(1);
        assertThat(vectorStoreService.search("住宿标准").get(0).filename()).isEqualTo("出差管理制度.docx");
    }
}
