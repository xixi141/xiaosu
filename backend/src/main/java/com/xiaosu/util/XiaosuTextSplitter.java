package com.xiaosu.util;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 中文文档切块器：
 * 1. 按空行分段（中文文档段落语义边界最清晰）
 * 2. 长段按 。！？； 与换行切句
 * 3. 合并短句直到接近 chunkSize 字符（600 汉字 ≈ 400 token）
 * 4. 块之间保留 overlap 字符重叠，避免切断上下文
 */
@Component
public class XiaosuTextSplitter {

    private static final String SENTENCE_BOUNDARY = "[。！？；\\n]+";

    public List<String> split(String text, int chunkSize, int overlap) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        List<String> sentences = new ArrayList<>();
        for (String paragraph : text.split("\\n\\s*\\n")) {
            if (paragraph.isBlank()) {
                continue;
            }
            for (String sentence : paragraph.split(SENTENCE_BOUNDARY)) {
                if (!sentence.isBlank()) {
                    sentences.add(sentence.trim());
                }
            }
        }

        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String sentence : sentences) {
            if (sentence.length() > chunkSize) {
                flush(chunks, current);
                // 超长句按 chunkSize 硬切
                for (int i = 0; i < sentence.length(); i += chunkSize - overlap) {
                    chunks.add(sentence.substring(i, Math.min(sentence.length(), i + chunkSize)));
                }
                continue;
            }
            if (current.length() + sentence.length() > chunkSize) {
                flush(chunks, current);
                if (overlap > 0 && !chunks.isEmpty()) {
                    String prev = chunks.get(chunks.size() - 1);
                    if (prev.length() > overlap) {
                        current.append(prev.substring(prev.length() - overlap));
                    }
                }
            }
            if (!current.isEmpty()) {
                current.append(' ');
            }
            current.append(sentence);
        }
        flush(chunks, current);
        return chunks;
    }

    private void flush(List<String> chunks, StringBuilder current) {
        if (!current.isEmpty()) {
            chunks.add(current.toString().trim());
            current.setLength(0);
        }
    }
}
