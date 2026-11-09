package com.vsl.social.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Tin nhắn riêng giữa 2 người dùng (chỉ lưu userId của identity-service).
 * Được nhắn khi một trong hai người đang theo dõi người kia (kiểm tra lúc gửi).
 */
@Entity
@Table(name = "direct_messages", indexes = {
        @Index(name = "idx_dm_sender_recipient", columnList = "sender_id, recipient_id"),
        @Index(name = "idx_dm_recipient_unread", columnList = "recipient_id, read_at")})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DirectMessage extends BaseEntity {

    public static final int MAX_LENGTH = 1000;

    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    @Column(name = "recipient_id", nullable = false)
    private Long recipientId;

    @Column(nullable = false, length = MAX_LENGTH)
    private String content;

    /** Lúc người nhận đọc; null = chưa đọc. */
    @Column(name = "read_at")
    private Instant readAt;
}
