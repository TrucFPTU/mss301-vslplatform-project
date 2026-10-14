package com.vsl.social.service;

import com.vsl.common.exception.ErrorCode;
import com.vsl.social.dto.BlogDetailResponse;
import com.vsl.social.dto.BlogRequest;
import com.vsl.social.entity.Blog;
import com.vsl.social.entity.BlogStatus;
import com.vsl.social.repository.BlogLikeRepository;
import com.vsl.social.repository.BlogNotificationRepository;
import com.vsl.social.repository.BlogRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

import static com.vsl.social.service.TestSupport.ADMIN;
import static com.vsl.social.service.TestSupport.AUTHOR;
import static com.vsl.social.service.TestSupport.OTHER;
import static com.vsl.social.service.TestSupport.assertAppError;
import static com.vsl.social.service.TestSupport.blog;
import static com.vsl.social.service.TestSupport.loginAs;
import static com.vsl.social.service.TestSupport.logout;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BlogServiceTest {

    @Mock
    BlogRepository blogRepository;
    @Mock
    BlogLikeRepository blogLikeRepository;
    @Mock
    BlogNotificationRepository notificationRepository;
    @InjectMocks
    BlogService blogService;

    @AfterEach
    void tearDown() {
        logout();
    }

    private void stubSaveReturnsArgument() {
        when(blogRepository.saveAndFlush(any(Blog.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // ---- Quy tắc hiển thị (ensureVisible) — điểm có nhiều phụ thuộc nhất ----
    @Nested
    class Visibility {

        @Test
        void publishedVisibleToGuest() {
            assertThatCode(() -> blogService.ensureVisible(blog(1L, AUTHOR, BlogStatus.PUBLISHED), null))
                    .doesNotThrowAnyException();
        }

        @Test
        void draftHiddenFromGuest() {
            assertAppError(() -> blogService.ensureVisible(blog(1L, AUTHOR, BlogStatus.DRAFT), null),
                    ErrorCode.BLOG_NOT_FOUND);
        }

        @Test
        void draftHiddenFromOtherUser() {
            loginAs(OTHER, "USER");
            assertAppError(() -> blogService.ensureVisible(blog(1L, AUTHOR, BlogStatus.DRAFT), OTHER),
                    ErrorCode.BLOG_NOT_FOUND);
        }

        @Test
        void hiddenBlogHiddenFromOtherUser() {
            loginAs(OTHER, "USER");
            assertAppError(() -> blogService.ensureVisible(blog(1L, AUTHOR, BlogStatus.HIDDEN), OTHER),
                    ErrorCode.BLOG_NOT_FOUND);
        }

        @Test
        void draftAndHiddenVisibleToAuthor() {
            loginAs(AUTHOR, "USER");
            assertThatCode(() -> blogService.ensureVisible(blog(1L, AUTHOR, BlogStatus.DRAFT), AUTHOR))
                    .doesNotThrowAnyException();
            assertThatCode(() -> blogService.ensureVisible(blog(1L, AUTHOR, BlogStatus.HIDDEN), AUTHOR))
                    .doesNotThrowAnyException();
        }

        @Test
        void draftVisibleToAdmin() {
            loginAs(ADMIN, "ADMIN");
            assertThatCode(() -> blogService.ensureVisible(blog(1L, AUTHOR, BlogStatus.DRAFT), ADMIN))
                    .doesNotThrowAnyException();
        }

        @Test
        void missingBlog() {
            when(blogRepository.findById(404L)).thenReturn(Optional.empty());
            assertAppError(() -> blogService.getBlog(404L), ErrorCode.BLOG_NOT_FOUND);
        }

        @Test
        void detail_likedByMeOnlyForLoggedInLiker() {
            when(blogRepository.findById(1L)).thenReturn(Optional.of(blog(1L, AUTHOR, BlogStatus.PUBLISHED)));
            when(blogLikeRepository.existsByBlogIdAndUserId(1L, OTHER)).thenReturn(true);

            assertThat(blogService.getDetail(1L, OTHER).likedByMe()).isTrue();
            assertThat(blogService.getDetail(1L, null).likedByMe()).isFalse();
        }
    }

    // ---- Tạo / sửa / xóa ----
    @Nested
    class Write {

        @Test
        void create_defaultsToPublishedAndTrimsTitle() {
            loginAs(AUTHOR, "USER");
            stubSaveReturnsArgument();

            BlogDetailResponse res = blogService.create(AUTHOR, new BlogRequest("  Hello  ", "body", null, null));

            assertThat(res.status()).isEqualTo(BlogStatus.PUBLISHED);
            assertThat(res.title()).isEqualTo("Hello");
            assertThat(res.authorId()).isEqualTo(AUTHOR);
        }

        @Test
        void create_hiddenRejectedForUser() {
            loginAs(AUTHOR, "USER");
            assertAppError(() -> blogService.create(AUTHOR, new BlogRequest("t", "c", null, BlogStatus.HIDDEN)),
                    ErrorCode.BLOG_HIDE_FORBIDDEN);
            verify(blogRepository, never()).saveAndFlush(any());
        }

        @Test
        void create_hiddenAllowedForAdmin() {
            loginAs(ADMIN, "ADMIN");
            stubSaveReturnsArgument();
            assertThat(blogService.create(ADMIN, new BlogRequest("t", "c", null, BlogStatus.HIDDEN)).status())
                    .isEqualTo(BlogStatus.HIDDEN);
        }

        @Test
        void update_rejectedForNonOwner() {
            loginAs(OTHER, "USER");
            when(blogRepository.findById(1L)).thenReturn(Optional.of(blog(1L, AUTHOR, BlogStatus.PUBLISHED)));

            assertAppError(() -> blogService.update(1L, OTHER, new BlogRequest("x", "y", null, null)),
                    ErrorCode.FORBIDDEN);
            verify(blogRepository, never()).saveAndFlush(any());
        }

        @Test
        void update_authorCannotUnhide() {
            loginAs(AUTHOR, "USER");
            Blog hidden = blog(1L, AUTHOR, BlogStatus.HIDDEN);
            when(blogRepository.findById(1L)).thenReturn(Optional.of(hidden));
            stubSaveReturnsArgument();

            BlogDetailResponse res = blogService.update(1L, AUTHOR,
                    new BlogRequest("New", "new body", null, BlogStatus.PUBLISHED));

            assertThat(res.status()).isEqualTo(BlogStatus.HIDDEN);
            assertThat(res.title()).isEqualTo("New");
        }

        @Test
        void update_adminCanHide() {
            loginAs(ADMIN, "ADMIN");
            when(blogRepository.findById(1L)).thenReturn(Optional.of(blog(1L, AUTHOR, BlogStatus.PUBLISHED)));
            stubSaveReturnsArgument();

            assertThat(blogService.update(1L, ADMIN, new BlogRequest("t", "c", null, BlogStatus.HIDDEN)).status())
                    .isEqualTo(BlogStatus.HIDDEN);
        }

        @Test
        void delete_removesNotificationsThenBlog() {
            loginAs(AUTHOR, "USER");
            Blog b = blog(1L, AUTHOR, BlogStatus.PUBLISHED);
            when(blogRepository.findById(1L)).thenReturn(Optional.of(b));

            blogService.delete(1L, AUTHOR);

            InOrder order = inOrder(notificationRepository, blogRepository);
            order.verify(notificationRepository).deleteByBlogId(1L);
            order.verify(blogRepository).delete(b);
        }

        @Test
        void delete_rejectedForNonOwner() {
            loginAs(OTHER, "USER");
            when(blogRepository.findById(1L)).thenReturn(Optional.of(blog(1L, AUTHOR, BlogStatus.PUBLISHED)));

            assertAppError(() -> blogService.delete(1L, OTHER), ErrorCode.FORBIDDEN);
            verify(blogRepository, never()).delete(any());
            verify(notificationRepository, never()).deleteByBlogId(anyLong());
        }
    }

    // ---- Danh sách ----
    @Nested
    class Listing {

        @Test
        void listPublished_usesSearchOnlyWithKeyword() {
            when(blogRepository.findByStatus(eq(BlogStatus.PUBLISHED), any(Pageable.class))).thenReturn(Page.empty());
            when(blogRepository.findByStatusAndTitleContainingIgnoreCase(eq(BlogStatus.PUBLISHED), eq("vsl"), any(Pageable.class)))
                    .thenReturn(Page.empty());

            blogService.listPublished("   ", 0, 10);
            blogService.listPublished("  vsl ", 0, 10);

            verify(blogRepository).findByStatus(eq(BlogStatus.PUBLISHED), any(Pageable.class));
            verify(blogRepository).findByStatusAndTitleContainingIgnoreCase(eq(BlogStatus.PUBLISHED), eq("vsl"), any(Pageable.class));
        }

        @Test
        void listByAuthor_authorSeesAllStatuses() {
            loginAs(AUTHOR, "USER");
            when(blogRepository.findByAuthorId(eq(AUTHOR), any(Pageable.class))).thenReturn(Page.empty());

            blogService.listByAuthor(AUTHOR, AUTHOR, 0, 10);

            verify(blogRepository).findByAuthorId(eq(AUTHOR), any(Pageable.class));
            verify(blogRepository, never()).findByAuthorIdAndStatus(any(), any(), any());
        }

        @Test
        void listByAuthor_othersSeePublishedOnly() {
            loginAs(OTHER, "USER");
            when(blogRepository.findByAuthorIdAndStatus(eq(AUTHOR), eq(BlogStatus.PUBLISHED), any(Pageable.class)))
                    .thenReturn(Page.empty());

            blogService.listByAuthor(AUTHOR, OTHER, 0, 10);

            verify(blogRepository, never()).findByAuthorId(any(), any());
        }
    }
}
