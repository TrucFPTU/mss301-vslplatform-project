package com.vsl.social.repository;

import com.vsl.social.entity.ReplyLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReplyLikeRepository extends JpaRepository<ReplyLike, Long> {

    Optional<ReplyLike> findByReplyIdAndUserId(Long replyId, Long userId);

    boolean existsByReplyIdAndUserId(Long replyId, Long userId);

    long countByReplyId(Long replyId);
}
