package com.vsl.social.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * blockerId chặn blockedId (userId của identity-service). Khi một trong hai người chặn người kia:
 * không thấy bài viết của nhau, không nhắn tin / theo dõi / tương tác được cho tới khi người chặn mở chặn.
 */
@Entity
@Table(name = "user_blocks",
        uniqueConstraints = @UniqueConstraint(name = "uk_user_blocks_pair", columnNames = {"blocker_id", "blocked_id"}),
        indexes = @Index(name = "idx_user_blocks_blocked", columnList = "blocked_id"))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserBlock extends BaseEntity {

    @Column(name = "blocker_id", nullable = false)
    private Long blockerId;

    @Column(name = "blocked_id", nullable = false)
    private Long blockedId;
}
