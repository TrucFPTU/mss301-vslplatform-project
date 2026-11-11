package com.vsl.identity.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Transactional Outbox: sự kiện cần báo cho service khác, ghi CÙNG transaction với thay đổi nghiệp vụ
 * (vd ẩn danh hóa user) nên không bao giờ có cảnh "đã đổi DB nhưng quên báo" hay ngược lại.
 * Mỗi service đích là một dòng riêng ({@link #destination}) -> mỗi bên được thử lại độc lập.
 * Relay ({@code OutboxRelayServiceImpl}) đọc các dòng PENDING tới hạn rồi gửi đi (at-least-once;
 * bên nhận phải idempotent).
 */
@Entity
@Table(name = "outbox_events",
        uniqueConstraints = @UniqueConstraint(name = "uk_outbox_event",
                columnNames = {"event_type", "aggregate_id", "destination"}),
        indexes = @Index(name = "idx_outbox_due", columnList = "status,next_attempt_at"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Loại sự kiện, vd {@code USER_DELETED}. */
    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    /** Id đối tượng của sự kiện (với USER_DELETED là userId). */
    @Column(name = "aggregate_id", nullable = false)
    private Long aggregateId;

    /** Service đích, vd {@code social-service}. */
    @Column(nullable = false, length = 50)
    private String destination;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OutboxStatus status;

    /** Số lần gửi thất bại. */
    @Column(nullable = false)
    private int attempts;

    @Column(name = "next_attempt_at", nullable = false)
    private LocalDateTime nextAttemptAt;

    @Column(name = "last_error", length = 500)
    private String lastError;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;
}
