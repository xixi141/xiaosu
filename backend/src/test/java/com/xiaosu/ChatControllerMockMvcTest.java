package com.xiaosu;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "xiaosu.rag.similarity-threshold=0.0")
@AutoConfigureMockMvc
@Import(TestAiConfig.class)
class ChatControllerMockMvcTest {

    @Autowired MockMvc mockMvc;

    @Test
    void askReturnsJsonStructure() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sessionId":"s1","userId":"tester","question":"年假几天？"}
                                """))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertThat(body).contains("\"status\"").contains("\"citations\"").contains("\"usage\"");
    }

    @Test
    void streamEmitsMetaTokenDoneSequence() throws Exception {
        // Flux 流式响应需要 async dispatch 模式才能拿到完整事件序列
        MvcResult result = mockMvc.perform(post("/api/chat/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sessionId":"s1","userId":"tester","question":"年假几天？"}
                                """))
                .andExpect(request().asyncStarted())
                .andReturn();

        MvcResult dispatched = mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andReturn();

        String body = dispatched.getResponse().getContentAsString();
        assertThat(body).contains("\"type\":\"meta\"");
        assertThat(body).contains("\"type\":\"done\"");
        assertThat(body).doesNotContain("\"type\":\"error\"");
    }

    @Test
    void refusalReturnsRefusedStatus() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sessionId":"s1","userId":"tester","question":"CEO 的家庭住址是？"}
                                """))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).contains("REFUSED");
    }
}
