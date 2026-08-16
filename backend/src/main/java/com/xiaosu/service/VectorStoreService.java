package com.xiaosu.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaosu.config.AppProperties;
import com.xiaosu.rag.RagContext;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;

/**
 * SimpleVectorStore 包装：
 * - 1.1.8 的 SimpleVectorStore 没有 setVectorStoreFilePath/getSimilarDocumentsCount，
 *   路径由 save(File)/load(File) 传入，计数通过读持久化 JSON 的条目数实现。
 * - 每次变更后显式 persist()（SimpleVectorStore 不会自动写盘）。
 */
@Service
public class VectorStoreService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final EmbeddingModel embeddingModel;
    private final AppProperties props;
    private SimpleVectorStore store;
    private String currentPath;

    public VectorStoreService(EmbeddingModel embeddingModel, AppProperties props) {
        this.embeddingModel = embeddingModel;
        this.props = props;
        this.store = SimpleVectorStore.builder(embeddingModel).build();
        this.currentPath = props.vectorStorePath();
        reload();
    }

    /** 测试用：换一个独立存储路径并清空内容 */
    public void reset(String path) {
        this.store = SimpleVectorStore.builder(embeddingModel).build();
        this.currentPath = path;
    }

    public void add(List<Document> docs) {
        store.add(docs);
        persist();
    }

    public void delete(List<String> ids) {
        store.delete(ids);
        persist();
    }

    public List<RagContext.RagHit> search(String query) {
        List<Document> docs = store.similaritySearch(SearchRequest.builder()
                .query(query)
                .topK(props.rag().topK())
                .similarityThreshold(props.rag().similarityThreshold())
                .build());
        return docs.stream()
                .map(d -> {
                    Object distance = d.getMetadata().get("distance");
                    double score = distance instanceof Number n ? n.doubleValue() : 0.0;
                    Object chunkIndex = d.getMetadata().getOrDefault("chunkIndex", 0);
                    return new RagContext.RagHit(
                            String.valueOf(d.getMetadata().get("documentId")),
                            String.valueOf(d.getMetadata().get("filename")),
                            chunkIndex instanceof Number n ? n.intValue() : 0,
                            d.getText(),
                            score);
                })
                .toList();
    }

    /** 已持久化的向量条数（读 JSON 顶层条目数；persist 后文件即最新） */
    public long count() {
        File file = new File(currentPath);
        if (!file.exists()) {
            return 0;
        }
        try {
            return objectMapper.readTree(file).size();
        } catch (Exception e) {
            return 0;
        }
    }

    public void persist() {
        store.save(new File(currentPath));
    }

    public void reload() {
        File file = new File(currentPath);
        if (file.exists()) {
            store.load(file);
        }
    }
}
