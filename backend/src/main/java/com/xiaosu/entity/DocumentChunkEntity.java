package com.xiaosu.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "document_chunk")
@Getter
@Setter
@NoArgsConstructor
public class DocumentChunkEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long documentId;

    /** SimpleVectorStore 中的 Document.id，用于删除文档时精确删除向量 */
    @Column(unique = true, nullable = false, length = 64)
    private String vectorId;

    @Column(nullable = false)
    private Integer chunkIndex;

    @Lob
    @Column(nullable = false)
    private String content;

    private Integer charCount;

    private LocalDateTime createdAt;
}
