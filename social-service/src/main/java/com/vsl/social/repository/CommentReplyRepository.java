package com.vsl.social.repository;

import com.vsl.social.entity.CommentReply;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface CommentReplyRepository extends JpaRepository<CommentReply, Long> {

    Page<CommentReply> findByCommentId(Long commentId, Pageable pageable);

    /** [commentId, số trả lời] cho cả trang bình luận một lần. */
    @Query("SELECT r.comment.id, COUNT(r) FROM CommentReply r WHERE r.comment.id IN :ids GROUP BY r.comment.id")
    List<Object[]> countByCommentIds(@Param("ids") Collection<Long> ids);
}
