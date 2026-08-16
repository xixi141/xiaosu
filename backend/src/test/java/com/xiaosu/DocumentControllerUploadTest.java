package com.xiaosu;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 上传文件名校验：拒绝含 U+FFFD 乱码字符的文件名。
 * 乱码来源：Windows 上 MSYS2 curl 把中文文件名按 GBK 发出，服务端按 UTF-8 解码
 * 后无效字节序列全部替换为 U+FFFD（实测见 AI_USAGE.md），入库后会污染列表展示和 RAG 引用。
 */
@SpringBootTest(properties = "xiaosu.rag.similarity-threshold=0.0")
@AutoConfigureMockMvc
@Import(TestAiConfig.class)
class DocumentControllerUploadTest {

    @Autowired MockMvc mockMvc;

    @Test
    void uploadRejectsMojibakeFilename() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "员工手�册.md", "text/markdown",
                "# 测试内容".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/documents").file(file))
                .andExpect(status().isBadRequest());
    }

    @Test
    void uploadAcceptsCleanChineseFilename() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "正常文档.md", "text/markdown",
                "# 正常内容".getBytes(StandardCharsets.UTF_8));

        MvcResult result = mockMvc.perform(multipart("/api/documents").file(file))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).contains("\"filename\":\"正常文档.md\"");
    }
}
