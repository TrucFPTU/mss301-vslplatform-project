package com.vsl.social.service.impl;

import com.vsl.common.exception.AppException;
import com.vsl.common.exception.ErrorCode;
import com.vsl.social.dto.CommentLikeResponse;
import com.vsl.social.entity.BlogComment;
import com.vsl.social.entity.CommentLike;
import com.vsl.social.entity.CommentReply;
import com.vsl.social.entity.ReplyLike;
import com.vsl.social.repository.BlogCommentRepository;
import com.vsl.social.repository.CommentLikeRepository;
import com.vsl.social.repository.CommentReplyRepository;
import com.vsl.social.repository.ReplyLikeRepository;
import com.vsl.social.service.BlogService;
import com.vsl.social.service.CommentLikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Like / bỏ like bình luận và trả lời. Idempotent giống LikeService của blog.
 * Số lượt tính trực tiếp qua countBy... (không lưu cột đếm -> không lo race condition).
 */
@Service
@RequiredArgsConstructor
public class CommentLikeServiceImpl implements CommentLikeService {

    private final BlogService blogService;
    private final BlogCommentRepository commentRepository;
    private final CommentReplyRepository replyRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final ReplyLikeRepository replyLikeRepository;

    @Override
    @Transactional
    public CommentLikeResponse likeComment(Long commentId, Long userId) {
        BlogComment comment = getVisibleComment(commentId, userId);
        if (!commentLikeRepository.existsByCommentIdAndUserId(commentId, userId)) {
            commentLikeRepository.save(CommentLike.builder().comment(comment).userId(userId).build());
        }
        return new CommentLikeResponse(commentId, true, commentLikeRepository.countByCommentId(commentId));
    }

    @Override
    @Transactional
    public CommentLikeResponse unlikeComment(Long commentId, Long userId) {
        getVisibleComment(commentId, userId);
        commentLikeRepository.findByCommentIdAndUserId(commentId, userId)
                .ifPresent(commentLikeRepository::delete);
        return new CommentLikeResponse(commentId, false, commentLikeRepository.countByCommentId(commentId));
    }

    @Override
    @Transactional
    public CommentLikeResponse likeReply(Long replyId, Long userId) {
        CommentReply reply = getVisibleReply(replyId, userId);
        if (!replyLikeRepository.existsByReplyIdAndUserId(replyId, userId)) {
            replyLikeRepository.save(ReplyLike.builder().reply(reply).userId(userId).build());
        }
        return new CommentLikeResponse(replyId, true, replyLikeRepository.countByReplyId(replyId));
    }

    @Override
    @Transactional
    public CommentLikeResponse unlikeReply(Long replyId, Long userId) {
        getVisibleReply(replyId, userId);
        replyLikeRepository.findByReplyIdAndUserId(replyId, userId)
                .ifPresent(replyLikeRepository::delete);
        return new CommentLikeResponse(replyId, false, replyLikeRepository.countByReplyId(replyId));
    }

    private BlogComment getVisibleComment(Long commentId, Long viewerId) {
        BlogComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new AppException(ErrorCode.COMMENT_NOT_FOUND));
        blogService.ensureVisible(comment.getBlog(), viewerId);
        return comment;
    }

    private CommentReply getVisibleReply(Long replyId, Long viewerId) {
        CommentReply reply = replyRepository.findById(replyId)
                .orElseThrow(() -> new AppException(ErrorCode.REPLY_NOT_FOUND));
        blogService.ensureVisible(reply.getComment().getBlog(), viewerId);
        return reply;
    }
}
