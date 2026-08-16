package com.xiaosu.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_log")
@Getter
@Setter
@NoArgsConstructor
public class ChatLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String sessionId;

    private String userId;

    private String conversationId;

    @Lob
    private String question;

    @Lob
    private String answer;

    private String model;

    private Integer inputTokens;

    private Integer outputTokens;

    private Integer totalTokens;

    /** JSON: [{name, arguments, resultSummary}] */
    @Lob
    private String toolCalls;

    /** JSON: [{documentId, filename, chunkIndex, snippet}] */
    @Lob
    private String citations;

    private Boolean isRefused;

    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(length = 512)
    private String errorMessage;

    private Long latencyMs;

    private LocalDateTime createdAt;

    public enum Status { SUCCESS, FALLBACK, FAILED, REFUSED }
}
