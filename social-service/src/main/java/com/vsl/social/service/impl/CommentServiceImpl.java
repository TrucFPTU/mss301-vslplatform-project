package com.vsl.social.service.impl;

import com.vsl.common.exception.AppException;
import com.vsl.common.exception.ErrorCode;
import com.vsl.common.response.PageResponse;
import com.vsl.social.client.AiModerationClient;
import com.vsl.social.client.IdentityClient;
import com.vsl.social.dto.CommentRequest;
import com.vsl.social.dto.CommentResponse;
import com.vsl.social.dto.ReplyResponse;
import com.vsl.social.dto.remote.RemoteDtos.ModerationResult;
import com.vsl.social.entity.Blog;
import com.vsl.social.entity.BlogComment;
import com.vsl.social.entity.CommentReply;
import com.vsl.social.entity.NotificationType;
import com.vsl.social.repository.BlogCommentRepository;
import com.vsl.social.repository.BlogRepository;
import com.vsl.social.repository.CommentReplyRepository;
import com.vsl.social.service.BlogService;
import com.vsl.social.service.CommentService;
import com.vsl.social.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * Bình luận & trả lời bình luận.
 * Sửa: chỉ người viết. Xóa: người viết, tác giả bài viết, hoặc ADMIN.
 * commentCount của bài chỉ đếm bình luận gốc (không đếm trả lời).
 * Bê từ monolith: kiểm duyệt AI + @mention (thông báo MENTION). Sửa so với bản cũ: kiểm duyệt cả khi SỬA
 * (trước đây đăng nội dung sạch rồi sửa thành nội dung vi phạm là lọt), không cho bình luận vào bài
 * không còn hiển thị.
 */
@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final BlogService blogService;
    private final BlogRepository blogRepository;
    private final BlogCommentRepository commentRepository;
    private final CommentReplyRepository replyRepository;
    private final NotificationService notificationService;
    private final AiModerationClient aiModerationClient;
    private final IdentityClient identityClient;
    private final SocialViews views;

    // ---- Bình luận ----

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CommentResponse> listComments(Long blogId, Long viewerId, int page, int size) {
        blogService.getVisibleBlog(blogId, viewerId);
        return SocialViews.page(commentRepository.findByBlogId(blogId, Paging.oldestFirst(page, size)),
                list -> views.comments(list, viewerId));
    }

    @Override
    @Transactional
    public CommentResponse addComment(Long blogId, Long userId, CommentRequest req) {
        Blog blog = blogService.getVisibleBlog(blogId, userId);
        String content = moderated(req.content());
        Long mentioned = validMention(req.mentionedUserId(), userId);
        BlogComment saved = commentRepository.save(BlogComment.builder()
                .blog(blog)
                .userId(userId)
                .content(content)
                .mentionedUserId(mentioned)
                .build());
        notificationService.notify(blog.getAuthorId(), userId, NotificationType.COMMENT, blogId, saved.getId(), null);
        if (mentioned != null && !Objects.equals(mentioned, blog.getAuthorId())) {
            notificationService.notify(mentioned, userId, NotificationType.MENTION, blogId, saved.getId(), null);
        }
        blogRepository.addCommentCount(blogId, 1);
        return views.comments(List.of(saved), userId).get(0);
    }

    @Override
    @Transactional
    public CommentResponse updateComment(Long commentId, Long userId, CommentRequest req) {
        BlogComment comment = getComment(commentId);
        AccessControl.requireOwner(userId, comment.getUserId());
        comment.setContent(moderated(req.content()));
        return views.comments(List.of(commentRepository.saveAndFlush(comment)), userId).get(0);
    }

    @Override
    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        BlogComment comment = getComment(commentId);
        Long blogId = comment.getBlog().getId();
        AccessControl.requireOwnerOrAdmin(userId, comment.getUserId(), comment.getBlog().getAuthorId());
        commentRepository.delete(comment);
        blogRepository.addCommentCount(blogId, -1);
    }

    // ---- Trả lời bình luận ----

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReplyResponse> listReplies(Long commentId, Long viewerId, int page, int size) {
        BlogComment comment = getComment(commentId);
        blogService.ensureVisible(comment.getBlog(), viewerId);
        return SocialViews.page(replyRepository.findByCommentId(commentId, Paging.oldestFirst(page, size)),
                list -> views.replies(list, viewerId));
    }

    @Override
    @Transactional
    public ReplyResponse addReply(Long commentId, Long userId, CommentRequest req) {
        BlogComment comment = getComment(commentId);
        blogService.ensureVisible(comment.getBlog(), userId);
        String content = moderated(req.content());
        Long mentioned = validMention(req.mentionedUserId(), userId);
        CommentReply saved = replyRepository.save(CommentReply.builder()
                .comment(comment)
                .userId(userId)
                .content(content)
                .mentionedUserId(mentioned)
                .build());
        Long blogId = comment.getBlog().getId();
        notificationService.notify(comment.getUserId(), userId, NotificationType.REPLY, blogId, commentId, saved.getId());
        if (mentioned != null && !Objects.equals(mentioned, comment.getUserId())) {
            notificationService.notify(mentioned, userId, NotificationType.MENTION, blogId, commentId, saved.getId());
        }
        return views.replies(List.of(saved), userId).get(0);
    }

    @Override
    @Transactional
    public ReplyResponse updateReply(Long replyId, Long userId, CommentRequest req) {
        CommentReply reply = getReply(replyId);
        AccessControl.requireOwner(userId, reply.getUserId());
        reply.setContent(moderated(req.content()));
        return views.replies(List.of(replyRepository.saveAndFlush(reply)), userId).get(0);
    }

    @Override
    @Transactional
    public void deleteReply(Long replyId, Long userId) {
        CommentReply reply = getReply(replyId);
        AccessControl.requireOwnerOrAdmin(userId, reply.getUserId(), reply.getComment().getBlog().getAuthorId());
        replyRepository.delete(reply);
    }

    /** Kiểm duyệt AI (admin bỏ qua); bị chặn -> COMMENT_REJECTED kèm lý do. Trả nội dung đã trim. */
    private String moderated(String raw) {
        String content = raw.trim();
        if (!AccessControl.isAdmin()) {
            ModerationResult result = aiModerationClient.moderate(null, content);
            if (!result.allowed()) {
                String reason = result.reason() == null || result.reason().isBlank()
                        ? ErrorCode.COMMENT_REJECTED.getDefaultMessage() : result.reason();
                throw new AppException(ErrorCode.COMMENT_REJECTED, reason);
            }
        }
        return content;
    }

    /** Người được nhắc phải tồn tại và không phải chính mình (nhắc chính mình -> bỏ qua). */
    private Long validMention(Long mentionedUserId, Long userId) {
        if (mentionedUserId == null || Objects.equals(mentionedUserId, userId)) {
            return null;
        }
        if (!identityClient.exists(mentionedUserId)) {
            throw new AppException(ErrorCode.USER_NOT_FOUND, "Người được nhắc tên không tồn tại");
        }
        return mentionedUserId;
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
