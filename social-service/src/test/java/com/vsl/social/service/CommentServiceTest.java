package com.vsl.social.service;

import com.vsl.common.exception.ErrorCode;
import com.vsl.social.dto.CommentRequest;
import com.vsl.social.dto.CommentResponse;
import com.vsl.social.entity.Blog;
import com.vsl.social.entity.BlogComment;
import com.vsl.social.entity.BlogStatus;
import com.vsl.social.entity.CommentReply;
import com.vsl.social.entity.NotificationType;
import com.vsl.social.repository.BlogCommentRepository;
import com.vsl.social.repository.BlogRepository;
import com.vsl.social.repository.CommentReplyRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static com.vsl.social.service.TestSupport.ADMIN;
import static com.vsl.social.service.TestSupport.AUTHOR;
import static com.vsl.social.service.TestSupport.OTHER;
import static com.vsl.social.service.TestSupport.assertAppError;
import static com.vsl.social.service.TestSupport.blog;
import static com.vsl.social.service.TestSupport.comment;
import static com.vsl.social.service.TestSupport.loginAs;
import static com.vsl.social.service.TestSupport.logout;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
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
    @InjectMocks
    CommentService commentService;

    private final Blog blog = blog(1L, AUTHOR, BlogStatus.PUBLISHED);

    @AfterEach
    void tearDown() {
        logout();
    }

    @Test
    void addComment_trimsNotifiesAuthorAndIncrementsCount() {
        when(blogService.getVisibleBlog(1L, OTHER)).thenReturn(blog);
        when(commentRepository.save(any(BlogComment.class))).thenAnswer(inv -> {
            BlogComment c = inv.getArgument(0);
            c.setId(10L);
            return c;
        });

        CommentResponse res = commentService.addComment(1L, OTHER, new CommentRequest("  hi  "));

        assertThat(res.content()).isEqualTo("hi");
        assertThat(res.blogId()).isEqualTo(1L);
        verify(notificationService).notify(AUTHOR, OTHER, NotificationType.COMMENT, 1L);
        verify(blogRepository).addCommentCount(1L, 1);
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
    void addReply_notifiesCommentOwner() {
        BlogComment c = comment(10L, blog, OTHER);
        when(commentRepository.findById(10L)).thenReturn(Optional.of(c));
        when(replyRepository.save(any(CommentReply.class))).thenAnswer(inv -> inv.getArgument(0));

        commentService.addReply(10L, AUTHOR, new CommentRequest("thanks"));

        verify(blogService).ensureVisible(blog, AUTHOR);
        verify(notificationService).notify(OTHER, AUTHOR, NotificationType.REPLY, 1L);
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
