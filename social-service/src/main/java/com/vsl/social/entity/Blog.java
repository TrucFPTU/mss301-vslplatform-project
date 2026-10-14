package com.vsl.social.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Bài viết. Tác giả lưu bằng Long authorId (User nằm ở identity-service).
 * Các bộ đếm like/comment/share được cập nhật bằng query cộng dồn trong BlogRepository.
 */
@Entity
@Table(name = "blogs", indexes = {
        @Index(name = "idx_blogs_author", columnList = "author_id"),
        @Index(name = "idx_blogs_status", columnList = "status")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Blog extends BaseEntity {

    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(length = 1000)
    private String thumbnailUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BlogStatus status;

    @Builder.Default
    @Column(nullable = false)
    private long likeCount = 0;

    @Builder.Default
    @Column(nullable = false)
    private long commentCount = 0;

    @Builder.Default
    @Column(nullable = false)
    private long shareCount = 0;
}
