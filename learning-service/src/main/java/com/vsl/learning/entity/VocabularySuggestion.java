package com.vsl.learning.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Một đề xuất từ vựng mới do người dùng gửi, cho admin xem (mang tính gợi ý).
 * Theo quy ước microservice: người đề xuất lưu bằng Long requesterId (KHÔNG @ManyToOne User,
 * vì User thuộc identity-service). Category cùng nằm trong learning-service nên vẫn giữ quan hệ JPA.
 */
@Entity
@Table(name = "vocabulary_suggestions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VocabularySuggestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "requester_id", nullable = false)
    private Long requesterId;

    @Column(nullable = false, length = 255)
    private String word;

    @Column(length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @Builder.Default
    private SuggestionStatus status = SuggestionStatus.PENDING;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;
}
