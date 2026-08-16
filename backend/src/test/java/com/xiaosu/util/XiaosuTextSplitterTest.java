package com.xiaosu.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class XiaosuTextSplitterTest {

    private final XiaosuTextSplitter splitter = new XiaosuTextSplitter();

    @Test
    void splitsBySentencesAtChunkBoundary() {
        // 契约：按 。！？； 切句、单空格合并，直到接近 chunkSize。
        // 句子长度：第一章 年假制度(7) / 年假句(20) / 第二章 考勤制度(7) / 上班时间(13) / 迟到(9)
        // chunkSize=25 时边界落在 7+1+20=28>25 与 7+1+13=21≤25 → 4 块
        String text = "第一章 年假制度\n\n员工工作满一年后，每年可享受 5 天带薪年假。\n\n第二章 考勤制度\n\n上班时间 9:00-18:00。迟到需在 OA 补卡。";

        List<String> chunks = splitter.split(text, 25, 0);

        assertThat(chunks).hasSize(4);
        assertThat(chunks.get(0)).isEqualTo("第一章 年假制度");
        assertThat(chunks.get(1)).contains("5 天带薪年假");
        assertThat(chunks.get(2)).contains("考勤制度").contains("9:00-18:00");
        assertThat(chunks.get(3)).isEqualTo("迟到需在 OA 补卡");
        // 所有内容不丢失
        assertThat(String.join(" ", chunks)).contains("年假制度").contains("迟到需在 OA 补卡");
    }

    @Test
    void mergesShortSentencesUntilChunkSize() {
        // 契约：5 句 × 3 字符 + 4 空格 = 19 字符 ≤ chunkSize=20 → 合并为 1 块（标点剥除、空格连接）
        String text = "第一条。第二条。第三条。第四条。第五条。";

        List<String> chunks = splitter.split(text, 20, 0);

        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0)).isEqualTo("第一条 第二条 第三条 第四条 第五条");
    }

    @Test
    void addsOverlapBetweenChunks() {
        String text = "第一句内容，比较长。第二句内容，也比较长。第三句内容，更加长。";

        List<String> chunks = splitter.split(text, 10, 3);

        assertThat(chunks).hasSizeGreaterThan(1);
        assertThat(chunks.get(1)).startsWith(chunks.get(0).substring(chunks.get(0).length() - 3));
    }

    @Test
    void ignoresEmptyAndBlankLines() {
        String text = "\n\n   \n\n只有这一段有效内容。\n\n";

        List<String> chunks = splitter.split(text, 100, 0);

        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0)).contains("只有这一段有效内容");
    }
}
