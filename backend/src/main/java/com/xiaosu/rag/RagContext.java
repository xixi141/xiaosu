package com.xiaosu.rag;

import java.util.List;

/** 一次检索的完整上下文：命中列表 + 拼给模型的编号文本 */
public record RagContext(List<RagHit> hits, String contextText) {
    public record RagHit(String documentId, String filename, int chunkIndex, String content, double score) {
    }
}
