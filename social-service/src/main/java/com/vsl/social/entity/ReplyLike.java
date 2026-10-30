package com.vsl.social.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/** Lượt thích một trả lời (reply). Mỗi (reply, user) chỉ 1 lượt (unique). */
@Entity
@Table(
        name = "reply_likes",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_reply_like_reply_user",
                columnNames = {"reply_id", "user_id"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReplyLike extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reply_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private CommentReply reply;

    @Column(name = "user_id", nullable = false)
    private Long userId;
}
