package com.xiaosu.rag;

import com.xiaosu.dto.Citation;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 引用组装：检索命中的每个切片生成一条 Citation（含文件名/切片序号/原文摘要），
 * 前端点击可跳转；钉钉卡片展示为「来源列表」。
 */
@Component
public class CitationAssembler {

    public List<Citation> fromHits(List<RagContext.RagHit> hits) {
        return hits.stream()
                .map(h -> new Citation(h.documentId(), h.filename(), h.chunkIndex(), snippetOf(h.content())))
                .toList();
    }

    /** 拼给模型的编号上下文：[1] 文件名\n内容 … */
    public String buildContextText(List<RagContext.RagHit> hits) {
        if (hits.isEmpty()) {
            return "（本次未检索到相关文档内容）";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < hits.size(); i++) {
            RagContext.RagHit h = hits.get(i);
            sb.append('[').append(i + 1).append("] ").append(h.filename()).append('\n')
              .append(h.content()).append("\n\n");
        }
        return sb.toString();
    }

    private String snippetOf(String content) {
        return content.length() > 150 ? content.substring(0, 150) + "…" : content;
    }
}
