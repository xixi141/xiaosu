package com.xiaosu.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "document")
@Getter
@Setter
@NoArgsConstructor
public class DocumentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String filename;

    private String fileType;

    private Long fileSize;

    @Column(unique = true, nullable = false, length = 64)
    private String sha256;

    @Enumerated(EnumType.STRING)
    private Status status;

    private Integer chunkCount;

    @Column(length = 512)
    private String errorMessage;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public enum Status { PARSING, READY, FAILED }
}
