package com.vsl.social.entity;

import com.vsl.common.text.VietnameseText;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Bài viết. Tác giả lưu bằng Long authorId (User nằm ở identity-service).
 * Các bộ đếm like/comment/share được cập nhật bằng query cộng dồn trong BlogRepository.
 * normalizedTitle / contentFingerprint: dạng đã chuẩn hóa (bỏ dấu, hoa-thường, khoảng trắng) để chặn
 * đăng lại bài trùng bằng truy vấn có index (monolith tải toàn bộ bài lên bộ nhớ mỗi lần đăng).
 */
@Entity
@Table(name = "blogs", indexes = {
        @Index(name = "idx_blogs_author", columnList = "author_id"),
        @Index(name = "idx_blogs_status", columnList = "status"),
        @Index(name = "idx_blogs_normalized_title", columnList = "normalized_title"),
        @Index(name = "idx_blogs_content_fingerprint", columnList = "content_fingerprint")
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

    /** Lý do admin gỡ bài (chỉ có khi HIDDEN) - hiển thị cho tác giả. */
    @Column(name = "deletion_reason", length = 1000)
    private String deletionReason;

    @Column(name = "normalized_title", length = 255)
    private String normalizedTitle;

    @Column(name = "content_fingerprint", length = 64)
    private String contentFingerprint;

    @Builder.Default
    @Column(nullable = false)
    private long likeCount = 0;

    @Builder.Default
    @Column(nullable = false)
    private long commentCount = 0;

    @Builder.Default
    @Column(nullable = false)
    private long shareCount = 0;

    /** Tính lại dạng chuẩn hóa của tiêu đề / nội dung (tự gọi trước khi lưu). */
    @PrePersist
    @PreUpdate
    public void refreshFingerprints() {
        String folded = VietnameseText.fold(title);
        this.normalizedTitle = folded.length() > 255 ? folded.substring(0, 255) : folded;
        this.contentFingerprint = VietnameseText.fingerprint(content);
    }
}
