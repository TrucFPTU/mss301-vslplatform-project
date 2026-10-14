package com.vsl.social.service;

import com.vsl.common.exception.AppException;
import com.vsl.common.exception.ErrorCode;
import com.vsl.common.response.PageResponse;
import com.vsl.social.dto.CommentRequest;
import com.vsl.social.dto.CommentResponse;
import com.vsl.social.dto.ReplyResponse;
import com.vsl.social.entity.Blog;
import com.vsl.social.entity.BlogComment;
import com.vsl.social.entity.CommentReply;
import com.vsl.social.entity.NotificationType;
import com.vsl.social.repository.BlogCommentRepository;
import com.vsl.social.repository.BlogRepository;
import com.vsl.social.repository.CommentReplyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bình luận & trả lời bình luận.
 * Sửa: chỉ người viết. Xóa: người viết, tác giả bài viết, hoặc ADMIN.
 * commentCount của bài chỉ đếm bình luận gốc (không đếm trả lời).
 */
@Service
@RequiredArgsConstructor
public class CommentService {

    private final BlogService blogService;
    private final BlogRepository blogRepository;
    private final BlogCommentRepository commentRepository;
    private final CommentReplyRepository replyRepository;
    private final NotificationService notificationService;

    // ---- Bình luận ----

    @Transactional(readOnly = true)
    public PageResponse<CommentResponse> listComments(Long blogId, Long viewerId, int page, int size) {
        blogService.getVisibleBlog(blogId, viewerId);
        return Paging.toResponse(
                commentRepository.findByBlogId(blogId, Paging.oldestFirst(page, size)),
                CommentResponse::from);
    }

    @Transactional
    public CommentResponse addComment(Long blogId, Long userId, CommentRequest req) {
        Blog blog = blogService.getVisibleBlog(blogId, userId);
        BlogComment saved = commentRepository.save(BlogComment.builder()
                .blog(blog)
                .userId(userId)
                .content(req.content().trim())
                .build());
        CommentResponse response = CommentResponse.from(saved);
        notificationService.notify(blog.getAuthorId(), userId, NotificationType.COMMENT, blogId);
        blogRepository.addCommentCount(blogId, 1);
        return response;
    }

    @Transactional
    public CommentResponse updateComment(Long commentId, Long userId, CommentRequest req) {
        BlogComment comment = getComment(commentId);
        AccessControl.requireOwner(userId, comment.getUserId());
        comment.setContent(req.content().trim());
        return CommentResponse.from(commentRepository.saveAndFlush(comment));
    }

    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        BlogComment comment = getComment(commentId);
        Long blogId = comment.getBlog().getId();
        AccessControl.requireOwnerOrAdmin(userId, comment.getUserId(), comment.getBlog().getAuthorId());
        commentRepository.delete(comment);
        blogRepository.addCommentCount(blogId, -1);
    }

    // ---- Trả lời bình luận ----

    @Transactional(readOnly = true)
    public PageResponse<ReplyResponse> listReplies(Long commentId, Long viewerId, int page, int size) {
        BlogComment comment = getComment(commentId);
        blogService.ensureVisible(comment.getBlog(), viewerId);
        return Paging.toResponse(
                replyRepository.findByCommentId(commentId, Paging.oldestFirst(page, size)),
                ReplyResponse::from);
    }

    @Transactional
    public ReplyResponse addReply(Long commentId, Long userId, CommentRequest req) {
        BlogComment comment = getComment(commentId);
        blogService.ensureVisible(comment.getBlog(), userId);
        CommentReply saved = replyRepository.save(CommentReply.builder()
                .comment(comment)
                .userId(userId)
                .content(req.content().trim())
                .build());
        notificationService.notify(comment.getUserId(), userId, NotificationType.REPLY, comment.getBlog().getId());
        return ReplyResponse.from(saved);
    }

    @Transactional
    public ReplyResponse updateReply(Long replyId, Long userId, CommentRequest req) {
        CommentReply reply = getReply(replyId);
        AccessControl.requireOwner(userId, reply.getUserId());
        reply.setContent(req.content().trim());
        return ReplyResponse.from(replyRepository.saveAndFlush(reply));
    }

    @Transactional
    public void deleteReply(Long replyId, Long userId) {
        CommentReply reply = getReply(replyId);
        AccessControl.requireOwnerOrAdmin(userId, reply.getUserId(), reply.getComment().getBlog().getAuthorId());
        replyRepository.delete(reply);
    }

    private BlogComment getComment(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new AppException(ErrorCode.COMMENT_NOT_FOUND));
    }

    private CommentReply getReply(Long replyId) {
        return replyRepository.findById(replyId)
                .orElseThrow(() -> new AppException(ErrorCode.REPLY_NOT_FOUND));
    }
}
