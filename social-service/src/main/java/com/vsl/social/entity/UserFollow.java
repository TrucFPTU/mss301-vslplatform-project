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

/** followerId theo dõi followeeId. Cả hai đều là userId của identity-service. */
@Entity
@Table(name = "user_follows",
        uniqueConstraints = @UniqueConstraint(name = "uk_user_follows_pair", columnNames = {"follower_id", "followee_id"}),
        indexes = @Index(name = "idx_user_follows_followee", columnList = "followee_id"))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserFollow extends BaseEntity {

    @Column(name = "follower_id", nullable = false)
    private Long followerId;

    @Column(name = "followee_id", nullable = false)
    private Long followeeId;
}
