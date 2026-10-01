package com.vsl.social.service;

import com.vsl.social.dto.LikeResponse;
import com.vsl.social.entity.Blog;
import com.vsl.social.entity.BlogLike;
import com.vsl.social.entity.BlogStatus;
import com.vsl.social.entity.NotificationType;
import com.vsl.social.repository.BlogLikeRepository;
import com.vsl.social.repository.BlogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static com.vsl.social.service.TestSupport.AUTHOR;
import static com.vsl.social.service.TestSupport.OTHER;
import static com.vsl.social.service.TestSupport.blog;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LikeServiceTest {

    @Mock
    BlogService blogService;
    @Mock
    BlogRepository blogRepository;
    @Mock
    BlogLikeRepository blogLikeRepository;
    @Mock
    NotificationService notificationService;
    @InjectMocks
    LikeService likeService;

    private Blog blogWithLikes(long likes) {
        Blog b = blog(1L, AUTHOR, BlogStatus.PUBLISHED);
        b.setLikeCount(likes);
        return b;
    }

    @Test
    void like_firstTime_savesCountsAndNotifies() {
        Blog b = blogWithLikes(0);
        when(blogService.getVisibleBlog(1L, OTHER)).thenReturn(b);
        when(blogLikeRepository.existsByBlogIdAndUserId(1L, OTHER)).thenReturn(false);
        when(blogService.getBlog(1L)).thenReturn(blogWithLikes(1));

        LikeResponse res = likeService.like(1L, OTHER);

        verify(blogLikeRepository).save(any(BlogLike.class));
        verify(blogRepository).addLikeCount(1L, 1);
        verify(notificationService).notify(AUTHOR, OTHER, NotificationType.LIKE, 1L);
        assertThat(res).isEqualTo(new LikeResponse(1L, true, 1));
    }

    @Test
    void like_again_isIdempotent() {
        when(blogService.getVisibleBlog(1L, OTHER)).thenReturn(blogWithLikes(1));
        when(blogLikeRepository.existsByBlogIdAndUserId(1L, OTHER)).thenReturn(true);
        when(blogService.getBlog(1L)).thenReturn(blogWithLikes(1));

        LikeResponse res = likeService.like(1L, OTHER);

        verify(blogLikeRepository, never()).save(any());
        verify(blogRepository, never()).addLikeCount(anyLong(), anyLong());
        verify(notificationService, never()).notify(any(), any(), any(), any());
        assertThat(res.likeCount()).isEqualTo(1);
    }

    @Test
    void unlike_existing_deletesAndDecrements() {
        BlogLike like = BlogLike.builder().userId(OTHER).build();
        when(blogService.getBlog(1L)).thenReturn(blogWithLikes(1), blogWithLikes(0));
        when(blogLikeRepository.findByBlogIdAndUserId(1L, OTHER)).thenReturn(Optional.of(like));

        LikeResponse res = likeService.unlike(1L, OTHER);

        verify(blogLikeRepository).delete(like);
        verify(blogRepository).addLikeCount(1L, -1);
        assertThat(res).isEqualTo(new LikeResponse(1L, false, 0));
    }

    @Test
    void unlike_notLiked_isNoOp() {
        when(blogService.getBlog(1L)).thenReturn(blogWithLikes(0));
        when(blogLikeRepository.findByBlogIdAndUserId(1L, OTHER)).thenReturn(Optional.empty());

        likeService.unlike(1L, OTHER);

        verify(blogLikeRepository, never()).delete(any());
        verify(blogRepository, never()).addLikeCount(anyLong(), anyLong());
    }
}
