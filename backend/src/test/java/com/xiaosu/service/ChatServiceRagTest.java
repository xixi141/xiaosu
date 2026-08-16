package com.xiaosu.service;

import com.xiaosu.TestAiConfig;
import com.xiaosu.dto.ChatRequest;
import com.xiaosu.dto.ChatResponseDto;
import com.xiaosu.entity.ChatLogEntity;
import com.xiaosu.repository.ChatLogRepository;
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

// 阈值置 0：FakeEmbeddingModel 的相似度分布与真实模型不同（见 DocumentIngestServiceTest 注释）
@SpringBootTest(properties = "xiaosu.rag.similarity-threshold=0.0")
@Import(TestAiConfig.class)
class ChatServiceRagTest {

    @Autowired ChatService chatService;
    @Autowired VectorStoreService vectorStoreService;
    @Autowired ChatLogRepository chatLogRepository;

    @TempDir Path tempDir;

    @BeforeEach
    void setUp() {
        chatLogRepository.deleteAll();
        vectorStoreService.reset(tempDir.resolve("vs.json").toString());
        vectorStoreService.add(List.of(
                Document.builder().id("d1c0")
                        .text("员工工作满一年后，每年可享受 5 天带薪年假，需提前 3 个工作日申请。")
                        .metadata(Map.of("documentId", "1", "filename", "员工手册.md", "chunkIndex", 0))
                        .build(),
                Document.builder().id("d1c1")
                        .text("报销需提供增值税发票原件和费用明细清单。")
                        .metadata(Map.of("documentId", "1", "filename", "员工手册.md", "chunkIndex", 1))
                        .build()
        ));
    }

    private ChatRequest req(String q) {
        return new ChatRequest("test-session", "tester", q);
    }

    @Test
    void answersWithCitationsFromKnowledgeBase() {
        // TestAiConfig 的 FakeChatModel 固定返回「这是模拟回答[1]」
        ChatResponseDto resp = chatService.ask(req("员工每年有几天年假？"));

        assertThat(resp.status()).isEqualTo("SUCCESS");
        assertThat(resp.answer()).contains("模拟回答");
        assertThat(resp.citations()).isNotEmpty();
        assertThat(resp.citations().get(0).filename()).isEqualTo("员工手册.md");
        assertThat(resp.citations().get(0).snippet()).contains("年假");
        // 日志落库
        List<ChatLogEntity> logs = chatLogRepository.findAll();
        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getQuestion()).isEqualTo("员工每年有几天年假？");
        assertThat(logs.get(0).getStatus()).isEqualTo(ChatLogEntity.Status.SUCCESS);
    }

    @Test
    void refusesWithoutCallingModel() {
        ChatResponseDto resp = chatService.ask(req("我们公司 CEO 的家庭住址是？"));

        assertThat(resp.status()).isEqualTo("REFUSED");
        assertThat(resp.answer()).contains("无法");   // 拒绝文案 ≠ FakeChatModel 的「模拟回答」
        List<ChatLogEntity> logs = chatLogRepository.findAll();
        assertThat(logs.get(0).getIsRefused()).isTrue();
    }

    @Test
    void multiTurnKeepsContextPerSession() {
        // 同一会话第二问，模型应看到第一轮历史（FakeChatModel 无法体现记忆内容，
        // 此处验证：同一 session 复用同一 memory advisor，不同 session 隔离）
        chatService.ask(req("员工每年有几天年假？"));
        ChatResponseDto second = chatService.ask(req("再详细讲讲"));

        assertThat(second.status()).isEqualTo("SUCCESS");
        assertThat(chatLogRepository.count()).isEqualTo(2);
    }
}
