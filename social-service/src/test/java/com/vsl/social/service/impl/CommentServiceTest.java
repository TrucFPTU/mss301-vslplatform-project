package com.vsl.social.service.impl;

import com.vsl.common.exception.ErrorCode;
import com.vsl.social.client.AiModerationClient;
import com.vsl.social.client.IdentityClient;
import com.vsl.social.dto.CommentRequest;
import com.vsl.social.dto.CommentResponse;
import com.vsl.social.dto.remote.RemoteDtos.ModerationResult;
import com.vsl.social.entity.Blog;
import com.vsl.social.entity.BlogComment;
import com.vsl.social.entity.BlogStatus;
import com.vsl.social.entity.CommentReply;
import com.vsl.social.entity.NotificationType;
import com.vsl.social.repository.BlogCommentRepository;
import com.vsl.social.repository.BlogLikeRepository;
import com.vsl.social.repository.BlogRepository;
import com.vsl.social.repository.CommentLikeRepository;
import com.vsl.social.repository.CommentReplyRepository;
import com.vsl.social.repository.ReplyLikeRepository;
import com.vsl.social.repository.UserFollowRepository;
import com.vsl.social.service.BlogService;
import com.vsl.social.service.NotificationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Map;
import java.util.Optional;

import static com.vsl.social.service.impl.TestSupport.ADMIN;
import static com.vsl.social.service.impl.TestSupport.AUTHOR;
import static com.vsl.social.service.impl.TestSupport.OTHER;
import static com.vsl.social.service.impl.TestSupport.assertAppError;
import static com.vsl.social.service.impl.TestSupport.blog;
import static com.vsl.social.service.impl.TestSupport.comment;
import static com.vsl.social.service.impl.TestSupport.loginAs;
import static com.vsl.social.service.impl.TestSupport.logout;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CommentServiceTest {

    static final Long STRANGER = 3L;

    @Mock
    BlogService blogService;
    @Mock
    BlogRepository blogRepository;
    @Mock
    BlogCommentRepository commentRepository;
    @Mock
    CommentReplyRepository replyRepository;
    @Mock
    NotificationService notificationService;
    @Mock
    AiModerationClient aiModerationClient;
    @Mock
    IdentityClient identityClient;
    @Mock
    BlogLikeRepository blogLikeRepository;
    @Mock
    UserFollowRepository followRepository;
    @Mock
    CommentLikeRepository commentLikeRepository;
    @Mock
    ReplyLikeRepository replyLikeRepository;

    CommentServiceImpl commentService;

    private final Blog blog = blog(1L, AUTHOR, BlogStatus.PUBLISHED);

    @BeforeEach
    void setUp() {
        SocialViews views = new SocialViews(identityClient, blogRepository, blogLikeRepository, followRepository,
                commentLikeRepository, replyLikeRepository, replyRepository);
        commentService = new CommentServiceImpl(blogService, blogRepository, commentRepository, replyRepository,
                notificationService, aiModerationClient, identityClient, views);
        when(aiModerationClient.moderate(any(), anyString())).thenReturn(new ModerationResult(true, "", true));
        when(identityClient.getSummaries(any())).thenReturn(Map.of());
        when(commentRepository.save(any(BlogComment.class))).thenAnswer(inv -> {
            BlogComment c = inv.getArgument(0);
            c.setId(10L);
            return c;
        });
        when(replyRepository.save(any(CommentReply.class))).thenAnswer(inv -> {
            CommentReply r = inv.getArgument(0);
            r.setId(20L);
            return r;
        });
    }

    @AfterEach
    void tearDown() {
        logout();
    }

    @Test
    void addComment_trimsNotifiesAuthorAndIncrementsCount() {
        when(blogService.getVisibleBlog(1L, OTHER)).thenReturn(blog);

        CommentResponse res = commentService.addComment(1L, OTHER, new CommentRequest("  hi  "));

        assertThat(res.content()).isEqualTo("hi");
        assertThat(res.blogId()).isEqualTo(1L);
        assertThat(res.user().userId()).isEqualTo(OTHER);
        verify(notificationService).notify(AUTHOR, OTHER, NotificationType.COMMENT, 1L, 10L, null);
        verify(blogRepository).addCommentCount(1L, 1);
    }

    @Test
    void addComment_rejectedByModeration_notSaved() {
        when(blogService.getVisibleBlog(1L, OTHER)).thenReturn(blog);
        when(aiModerationClient.moderate(isNull(), eq("mua ngay giá rẻ")))
                .thenReturn(new ModerationResult(false, "Quảng cáo", true));

        assertAppError(() -> commentService.addComment(1L, OTHER, new CommentRequest("mua ngay giá rẻ")),
                ErrorCode.COMMENT_REJECTED);
        verify(commentRepository, never()).save(any());
        verify(blogRepository, never()).addCommentCount(anyLong(), anyLong());
    }

    @Test
    void addComment_mentionNotifiesMentionedUser() {
        when(blogService.getVisibleBlog(1L, OTHER)).thenReturn(blog);
        when(identityClient.exists(STRANGER)).thenReturn(true);

        CommentResponse res = commentService.addComment(1L, OTHER, new CommentRequest("chào @bạn", STRANGER));

        assertThat(res.mentionedUser().userId()).isEqualTo(STRANGER);
        verify(notificationService).notify(STRANGER, OTHER, NotificationType.MENTION, 1L, 10L, null);
    }

    @Test
    void addComment_mentionBlogAuthor_singleMentionNotification() {
        when(blogService.getVisibleBlog(1L, OTHER)).thenReturn(blog);
        when(identityClient.exists(AUTHOR)).thenReturn(true);

        commentService.addComment(1L, OTHER, new CommentRequest("chào tác giả", AUTHOR));

        verify(notificationService).notify(AUTHOR, OTHER, NotificationType.MENTION, 1L, 10L, null);
        verify(notificationService, never()).notify(AUTHOR, OTHER, NotificationType.COMMENT, 1L, 10L, null);
    }

    @Test
    void addComment_mentionUnknownUser_rejected() {
        when(blogService.getVisibleBlog(1L, OTHER)).thenReturn(blog);
        when(identityClient.exists(404L)).thenReturn(false);

        assertAppError(() -> commentService.addComment(1L, OTHER, new CommentRequest("hi", 404L)), ErrorCode.USER_NOT_FOUND);
    }

    @Test
    void addComment_mentionSelf_ignored() {
        when(blogService.getVisibleBlog(1L, OTHER)).thenReturn(blog);

        CommentResponse res = commentService.addComment(1L, OTHER, new CommentRequest("hi", OTHER));

        assertThat(res.mentionedUser()).isNull();
        verify(notificationService, never()).notify(any(), any(), eq(NotificationType.MENTION), any(), any(), any());
    }

    @Test
    void updateComment_onlyOwner() {
        loginAs(AUTHOR, "USER");
        when(commentRepository.findById(10L)).thenReturn(Optional.of(comment(10L, blog, OTHER)));

        // Tác giả bài viết cũng KHÔNG được sửa bình luận của người khác
        assertAppError(() -> commentService.updateComment(10L, AUTHOR, new CommentRequest("x")), ErrorCode.FORBIDDEN);
        verify(commentRepository, never()).saveAndFlush(any());
    }

    @Test
    void updateComment_isModeratedAgain() {
        loginAs(OTHER, "USER");
        when(commentRepository.findById(10L)).thenReturn(Optional.of(comment(10L, blog, OTHER)));
        when(aiModerationClient.moderate(isNull(), eq("nội dung xấu")))
                .thenReturn(new ModerationResult(false, "Thô tục", true));

        assertAppError(() -> commentService.updateComment(10L, OTHER, new CommentRequest("nội dung xấu")),
                ErrorCode.COMMENT_REJECTED);
        verify(commentRepository, never()).saveAndFlush(any());
    }

    @Test
    void deleteComment_allowedForBlogAuthor() {
        loginAs(AUTHOR, "USER");
        BlogComment c = comment(10L, blog, OTHER);
        when(commentRepository.findById(10L)).thenReturn(Optional.of(c));

        commentService.deleteComment(10L, AUTHOR);

        verify(commentRepository).delete(c);
        verify(blogRepository).addCommentCount(1L, -1);
    }

    @Test
    void deleteComment_allowedForAdmin() {
        loginAs(ADMIN, "ADMIN");
        BlogComment c = comment(10L, blog, OTHER);
        when(commentRepository.findById(10L)).thenReturn(Optional.of(c));

        commentService.deleteComment(10L, ADMIN);

        verify(commentRepository).delete(c);
    }

    @Test
    void deleteComment_rejectedForStranger() {
        loginAs(STRANGER, "USER");
        when(commentRepository.findById(10L)).thenReturn(Optional.of(comment(10L, blog, OTHER)));

        assertAppError(() -> commentService.deleteComment(10L, STRANGER), ErrorCode.FORBIDDEN);
        verify(commentRepository, never()).delete(any());
        verify(blogRepository, never()).addCommentCount(anyLong(), anyLong());
    }

    @Test
    void missingComment() {
        when(commentRepository.findById(404L)).thenReturn(Optional.empty());
        assertAppError(() -> commentService.updateComment(404L, OTHER, new CommentRequest("x")),
                ErrorCode.COMMENT_NOT_FOUND);
    }

    @Test
    void addReply_notifiesCommentOwnerWithIds() {
        BlogComment c = comment(10L, blog, OTHER);
        when(commentRepository.findById(10L)).thenReturn(Optional.of(c));

        commentService.addReply(10L, AUTHOR, new CommentRequest("thanks"));

        verify(blogService).ensureVisible(blog, AUTHOR);
        verify(notificationService).notify(OTHER, AUTHOR, NotificationType.REPLY, 1L, 10L, 20L);
    }

    @Test
    void deleteReply_rejectedForStranger() {
        loginAs(STRANGER, "USER");
        CommentReply r = CommentReply.builder().comment(comment(10L, blog, OTHER)).userId(OTHER).content("r").build();
        r.setId(20L);
        when(replyRepository.findById(20L)).thenReturn(Optional.of(r));

        assertAppError(() -> commentService.deleteReply(20L, STRANGER), ErrorCode.FORBIDDEN);
        verify(replyRepository, never()).delete(any());
    }

    @Test
    void missingReply() {
        when(replyRepository.findById(404L)).thenReturn(Optional.empty());
        assertAppError(() -> commentService.deleteReply(404L, OTHER), ErrorCode.REPLY_NOT_FOUND);
    }
}
