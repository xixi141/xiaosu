package com.xiaosu.service;

import com.xiaosu.ToolTestConfig;
import com.xiaosu.dto.ChatRequest;
import com.xiaosu.dto.ChatResponseDto;
import com.xiaosu.entity.ChatLogEntity;
import com.xiaosu.repository.ChatLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Mock LLM 触发真实工具执行的 agent loop 测试：
 * 模型第一轮返回 order_stats 的 tool_call → Spring AI 执行 OrderTool（真实统计 mock 数据）
 * → 结果回填 → 第二轮得到最终回答。全程不触网。
 */
@SpringBootTest(properties = "xiaosu.rag.similarity-threshold=0.0")
@Import(ToolTestConfig.class)
class ChatServiceToolTest {

    @Autowired ChatService chatService;
    @Autowired ChatLogRepository chatLogRepository;
    @Autowired VectorStoreService vectorStoreService;

    @TempDir Path tempDir;

    @BeforeEach
    void setUp() {
        chatLogRepository.deleteAll();
        vectorStoreService.reset(tempDir.resolve("vs.json").toString());
    }

    @Test
    void modelDecidesToCallOrderToolAndGetsRealStats() {
        ChatResponseDto resp = chatService.ask(new ChatRequest("s1", "tester", "上周一共多少订单？"));

        assertThat(resp.status()).isEqualTo("SUCCESS");
        assertThat(resp.answer()).contains("17 笔");          // 最终回答来自脚本
        assertThat(resp.toolCalls()).hasSize(1);              // 工具调用被记录
        assertThat(resp.toolCalls().get(0).name()).isEqualTo("order_stats");
        assertThat(resp.toolCalls().get(0).resultSummary()).contains("17 笔有效订单");  // 工具真实执行结果
        // 日志落库含工具信息
        ChatLogEntity entry = chatLogRepository.findAll().get(0);
        assertThat(entry.getToolCalls()).contains("order_stats");
    }
}
