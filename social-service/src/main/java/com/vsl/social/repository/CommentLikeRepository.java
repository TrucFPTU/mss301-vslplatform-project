package com.vsl.social.repository;

import com.vsl.social.entity.CommentLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.Collection;
import java.util.List;

public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {

    Optional<CommentLike> findByCommentIdAndUserId(Long commentId, Long userId);

    boolean existsByCommentIdAndUserId(Long commentId, Long userId);

    long countByCommentId(Long commentId);

    /** [commentId, số like] cho cả trang bình luận một lần. */
    @Query("SELECT l.comment.id, COUNT(l) FROM CommentLike l WHERE l.comment.id IN :ids GROUP BY l.comment.id")
    List<Object[]> countByCommentIds(@Param("ids") Collection<Long> ids);

    @Query("SELECT l.comment.id FROM CommentLike l WHERE l.userId = :userId AND l.comment.id IN :ids")
    List<Long> findLikedCommentIds(@Param("userId") Long userId, @Param("ids") Collection<Long> ids);
}
