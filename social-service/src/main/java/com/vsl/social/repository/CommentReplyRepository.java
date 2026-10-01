package com.vsl.social.repository;

import com.vsl.social.entity.CommentReply;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentReplyRepository extends JpaRepository<CommentReply, Long> {

    Page<CommentReply> findByCommentId(Long commentId, Pageable pageable);
}
