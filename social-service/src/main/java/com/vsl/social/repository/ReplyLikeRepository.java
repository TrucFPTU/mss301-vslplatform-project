package com.vsl.social.repository;

import com.vsl.social.entity.ReplyLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.Collection;
import java.util.List;

public interface ReplyLikeRepository extends JpaRepository<ReplyLike, Long> {

    Optional<ReplyLike> findByReplyIdAndUserId(Long replyId, Long userId);

    boolean existsByReplyIdAndUserId(Long replyId, Long userId);

    long countByReplyId(Long replyId);

    @Query("SELECT l.reply.id, COUNT(l) FROM ReplyLike l WHERE l.reply.id IN :ids GROUP BY l.reply.id")
    List<Object[]> countByReplyIds(@Param("ids") Collection<Long> ids);

    @Query("SELECT l.reply.id FROM ReplyLike l WHERE l.userId = :userId AND l.reply.id IN :ids")
    List<Long> findLikedReplyIds(@Param("userId") Long userId, @Param("ids") Collection<Long> ids);
}
