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
 * Thông báo cho recipientId khi actorId tương tác (like, comment, follow...).
 * blogId lưu dạng số (không FK) — khi xóa bài, BlogService xóa các thông báo liên quan.
 */
@Entity
@Table(name = "blog_notifications", indexes = @Index(name = "idx_blog_notifications_recipient", columnList = "recipient_id"))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlogNotification extends BaseEntity {

    @Column(name = "recipient_id", nullable = false)
    private Long recipientId;

    @Column(name = "actor_id", nullable = false)
    private Long actorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationType type;

    @Column(name = "blog_id")
    private Long blogId;

    /** Bình luận / trả lời liên quan (để mở đúng chỗ khi bấm thông báo). */
    @Column(name = "comment_id")
    private Long commentId;

    @Column(name = "reply_id")
    private Long replyId;

    @Builder.Default
    @Column(name = "is_read", nullable = false)
    private boolean read = false;
}
