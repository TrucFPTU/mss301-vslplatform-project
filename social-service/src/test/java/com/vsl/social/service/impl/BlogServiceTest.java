package com.vsl.social.service.impl;

import com.vsl.common.exception.ErrorCode;
import com.vsl.common.storage.StorageService;
import com.vsl.social.client.IdentityClient;
import com.vsl.social.dto.BlogDetailResponse;
import com.vsl.social.dto.BlogRequest;
import com.vsl.social.dto.remote.RemoteDtos.UserSummary;
import com.vsl.social.entity.Blog;
import com.vsl.social.entity.BlogReport;
import com.vsl.social.entity.BlogStatus;
import com.vsl.social.entity.ReportStatus;
import com.vsl.social.repository.BlogLikeRepository;
import com.vsl.social.repository.BlogNotificationRepository;
import com.vsl.social.repository.BlogReportRepository;
import com.vsl.social.repository.BlogRepository;
import com.vsl.social.repository.BlogShareRepository;
import com.vsl.social.repository.CommentLikeRepository;
import com.vsl.social.repository.CommentReplyRepository;
import com.vsl.social.repository.ReplyLikeRepository;
import com.vsl.social.repository.UserFollowRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.vsl.social.service.impl.TestSupport.ADMIN;
import static com.vsl.social.service.impl.TestSupport.AUTHOR;
import static com.vsl.social.service.impl.TestSupport.OTHER;
import static com.vsl.social.service.impl.TestSupport.assertAppError;
import static com.vsl.social.service.impl.TestSupport.blog;
import static com.vsl.social.service.impl.TestSupport.loginAs;
import static com.vsl.social.service.impl.TestSupport.logout;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BlogServiceTest {

    @Mock
    BlogRepository blogRepository;
    @Mock
    BlogLikeRepository blogLikeRepository;
    @Mock
    BlogNotificationRepository notificationRepository;
    @Mock
    BlogReportRepository reportRepository;
    @Mock
    BlogShareRepository shareRepository;
    @Mock
    UserFollowRepository followRepository;
    @Mock
    CommentLikeRepository commentLikeRepository;
    @Mock
    ReplyLikeRepository replyLikeRepository;
    @Mock
    CommentReplyRepository replyRepository;
    @Mock
    BlogPublishGuard publishGuard;
    @Mock
    IdentityClient identityClient;
    @Mock
    ObjectProvider<StorageService> storageProvider;
    @Mock
    StorageService storage;

    BlogServiceImpl blogService;

    @BeforeEach
    void setUp() {
        // SocialViews thật (repo mock) -> test cả dữ liệu hiển thị, không chỉ luồng gọi.
        SocialViews views = new SocialViews(identityClient, blogRepository, blogLikeRepository, followRepository,
                commentLikeRepository, replyLikeRepository, replyRepository);
        blogService = new BlogServiceImpl(blogRepository, blogLikeRepository, notificationRepository, reportRepository,
                shareRepository, publishGuard, views, identityClient, storageProvider);
        when(identityClient.getSummaries(any())).thenReturn(Map.of(
                AUTHOR, new UserSummary(AUTHOR, "author", "Tác Giả", "http://a/1.png")));
        when(storageProvider.getIfAvailable()).thenReturn(storage);
    }

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
        void detail_likedByMeAndAuthorInfo() {
            when(blogRepository.findById(1L)).thenReturn(Optional.of(blog(1L, AUTHOR, BlogStatus.PUBLISHED)));
            when(blogLikeRepository.findLikedBlogIds(eq(OTHER), any())).thenReturn(List.of(1L));

            BlogDetailResponse liked = blogService.getDetail(1L, OTHER);
            assertThat(liked.likedByMe()).isTrue();
            assertThat(liked.author().displayName()).isEqualTo("Tác Giả");
            assertThat(blogService.getDetail(1L, null).likedByMe()).isFalse();
        }

        @Test
        void detail_friendWithAuthorWhenMutualFollow() {
            when(blogRepository.findById(1L)).thenReturn(Optional.of(blog(1L, AUTHOR, BlogStatus.PUBLISHED)));
            when(followRepository.findFolloweeIdsAmong(eq(OTHER), any())).thenReturn(List.of(AUTHOR));
            when(followRepository.findFollowerIdsAmong(eq(OTHER), any())).thenReturn(List.of(AUTHOR));

            BlogDetailResponse res = blogService.getDetail(1L, OTHER);

            assertThat(res.followedAuthor()).isTrue();
            assertThat(res.friendWithAuthor()).isTrue();
        }

        @Test
        void deletionReason_onlyForAuthorAndAdmin() {
            Blog hidden = blog(1L, AUTHOR, BlogStatus.HIDDEN);
            hidden.setDeletionReason("Spam");
            when(blogRepository.findById(1L)).thenReturn(Optional.of(hidden));

            loginAs(AUTHOR, "USER");
            assertThat(blogService.getDetail(1L, AUTHOR).deletionReason()).isEqualTo("Spam");
            loginAs(ADMIN, "ADMIN");
            assertThat(blogService.getDetail(1L, ADMIN).deletionReason()).isEqualTo("Spam");
        }
    }

    // ---- Tạo / sửa / xóa ----
    @Nested
    class Write {

        @Test
        void create_defaultsToPublishedTrimsTitleAndRunsGuard() {
            loginAs(AUTHOR, "USER");
            stubSaveReturnsArgument();

            BlogDetailResponse res = blogService.create(AUTHOR, new BlogRequest("  Hello  ", "body", null));

            assertThat(res.status()).isEqualTo(BlogStatus.PUBLISHED);
            assertThat(res.title()).isEqualTo("Hello");
            assertThat(res.author().userId()).isEqualTo(AUTHOR);
            verify(publishGuard).checkBeforePublish("Hello", "body", null);
        }

        @Test
        void create_draftSkipsGuard() {
            loginAs(AUTHOR, "USER");
            stubSaveReturnsArgument();

            blogService.create(AUTHOR, new BlogRequest("t", "c", BlogStatus.DRAFT));

            verify(publishGuard, never()).checkBeforePublish(anyString(), anyString(), any());
        }

        @Test
        void create_adminSkipsGuard() {
            loginAs(ADMIN, "ADMIN");
            stubSaveReturnsArgument();

            blogService.create(ADMIN, new BlogRequest("t", "c", BlogStatus.PUBLISHED));

            verify(publishGuard, never()).checkBeforePublish(anyString(), anyString(), any());
        }

        @Test
        void create_hiddenRejectedEvenForAdmin() {
            loginAs(ADMIN, "ADMIN");
            assertAppError(() -> blogService.create(ADMIN, new BlogRequest("t", "c", BlogStatus.HIDDEN)),
                    ErrorCode.BLOG_HIDE_FORBIDDEN);
            verify(blogRepository, never()).saveAndFlush(any());
        }

        @Test
        void update_rejectedForNonOwner() {
            loginAs(OTHER, "USER");
            when(blogRepository.findById(1L)).thenReturn(Optional.of(blog(1L, AUTHOR, BlogStatus.PUBLISHED)));

            assertAppError(() -> blogService.update(1L, OTHER, new BlogRequest("x", "y", null)), ErrorCode.FORBIDDEN);
            verify(blogRepository, never()).saveAndFlush(any());
        }

        @Test
        void update_hiddenLockedForAuthor() {
            loginAs(AUTHOR, "USER");
            when(blogRepository.findById(1L)).thenReturn(Optional.of(blog(1L, AUTHOR, BlogStatus.HIDDEN)));

            assertAppError(() -> blogService.update(1L, AUTHOR, new BlogRequest("New", "new", BlogStatus.PUBLISHED)),
                    ErrorCode.BLOG_HIDDEN_LOCKED);
        }

        @Test
        void update_unchangedPublishedSkipsGuard_changedRunsGuardExcludingSelf() {
            loginAs(AUTHOR, "USER");
            Blog b = blog(1L, AUTHOR, BlogStatus.PUBLISHED);
            when(blogRepository.findById(1L)).thenReturn(Optional.of(b));
            stubSaveReturnsArgument();

            blogService.update(1L, AUTHOR, new BlogRequest("Title 1", "Content 1", null));
            verify(publishGuard, never()).checkBeforePublish(anyString(), anyString(), any());

            blogService.update(1L, AUTHOR, new BlogRequest("Title 1", "Nội dung mới", null));
            verify(publishGuard).checkBeforePublish("Title 1", "Nội dung mới", 1L);
        }

        @Test
        void update_draftToPublishedRunsGuard() {
            loginAs(AUTHOR, "USER");
            when(blogRepository.findById(1L)).thenReturn(Optional.of(blog(1L, AUTHOR, BlogStatus.DRAFT)));
            stubSaveReturnsArgument();

            blogService.update(1L, AUTHOR, new BlogRequest("Title 1", "Content 1", BlogStatus.PUBLISHED));

            verify(publishGuard).checkBeforePublish("Title 1", "Content 1", 1L);
        }

        @Test
        void delete_removesNotificationsThenBlogThenThumbnail() {
            loginAs(AUTHOR, "USER");
            Blog b = blog(1L, AUTHOR, BlogStatus.PUBLISHED);
            b.setThumbnailUrl("http://minio/blog-thumbnails/blog-1/x.jpg");
            when(blogRepository.findById(1L)).thenReturn(Optional.of(b));

            blogService.delete(1L, AUTHOR);

            InOrder order = inOrder(notificationRepository, blogRepository, storage);
            order.verify(notificationRepository).deleteByBlogId(1L);
            order.verify(blogRepository).delete(b);
            order.verify(storage).deleteByUrl(BlogServiceImpl.THUMBNAIL_BUCKET, "http://minio/blog-thumbnails/blog-1/x.jpg");
        }

        @Test
        void delete_blockedWhileReportPending() {
            loginAs(AUTHOR, "USER");
            when(blogRepository.findById(1L)).thenReturn(Optional.of(blog(1L, AUTHOR, BlogStatus.PUBLISHED)));
            when(reportRepository.existsByBlogIdAndStatus(1L, ReportStatus.PENDING)).thenReturn(true);

            assertAppError(() -> blogService.delete(1L, AUTHOR), ErrorCode.BLOG_UNDER_REPORT);
            verify(blogRepository, never()).delete(any());
        }

        @Test
        void delete_blockedWhenHidden_evenForAdmin() {
            loginAs(ADMIN, "ADMIN");
            when(blogRepository.findById(1L)).thenReturn(Optional.of(blog(1L, AUTHOR, BlogStatus.HIDDEN)));

            assertAppError(() -> blogService.delete(1L, ADMIN), ErrorCode.BLOG_UNDER_REPORT);
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

    // ---- Quản trị + ảnh bìa ----
    @Nested
    class AdminAndThumbnail {

        @Test
        void hide_setsReasonAndResolvesPendingReports() {
            loginAs(ADMIN, "ADMIN");
            Blog b = blog(1L, AUTHOR, BlogStatus.PUBLISHED);
            BlogReport r1 = BlogReport.builder().blog(b).reporterId(OTHER).reason("x").status(ReportStatus.PENDING).build();
            BlogReport r2 = BlogReport.builder().blog(b).reporterId(3L).reason("y").status(ReportStatus.PENDING).build();
            when(blogRepository.findById(1L)).thenReturn(Optional.of(b));
            when(reportRepository.findByBlogIdAndStatus(1L, ReportStatus.PENDING)).thenReturn(List.of(r1, r2));
            stubSaveReturnsArgument();

            BlogDetailResponse res = blogService.hide(1L, ADMIN, "  Quảng cáo  ");

            assertThat(res.status()).isEqualTo(BlogStatus.HIDDEN);
            assertThat(res.deletionReason()).isEqualTo("Quảng cáo");
            assertThat(r1.getStatus()).isEqualTo(ReportStatus.RESOLVED);
            assertThat(r2.getStatus()).isEqualTo(ReportStatus.RESOLVED);
        }

        @Test
        void hide_requiresAdmin() {
            loginAs(AUTHOR, "USER");
            assertAppError(() -> blogService.hide(1L, AUTHOR, "x"), ErrorCode.FORBIDDEN);
        }

        @Test
        void restore_onlyHidden() {
            loginAs(ADMIN, "ADMIN");
            when(blogRepository.findById(1L)).thenReturn(Optional.of(blog(1L, AUTHOR, BlogStatus.PUBLISHED)));

            assertAppError(() -> blogService.restore(1L, ADMIN), ErrorCode.VALIDATION_ERROR);
        }

        @Test
        void restore_clearsReason() {
            loginAs(ADMIN, "ADMIN");
            Blog b = blog(1L, AUTHOR, BlogStatus.HIDDEN);
            b.setDeletionReason("Spam");
            when(blogRepository.findById(1L)).thenReturn(Optional.of(b));
            stubSaveReturnsArgument();

            BlogDetailResponse res = blogService.restore(1L, ADMIN);

            assertThat(res.status()).isEqualTo(BlogStatus.PUBLISHED);
            assertThat(b.getDeletionReason()).isNull();
        }

        @Test
        void uploadThumbnail_rejectsNonImage() {
            loginAs(AUTHOR, "USER");
            when(blogRepository.findById(1L)).thenReturn(Optional.of(blog(1L, AUTHOR, BlogStatus.PUBLISHED)));
            MockMultipartFile html = new MockMultipartFile("image", "x.html", "text/html", "<script>".getBytes());

            assertAppError(() -> blogService.uploadThumbnail(1L, AUTHOR, html), ErrorCode.INVALID_FILE);
            verify(storage, never()).upload(anyString(), anyString(), any(InputStream.class), anyLong(), anyString());
        }

        @Test
        void uploadThumbnail_storesAndReplacesOld() {
            loginAs(AUTHOR, "USER");
            Blog b = blog(1L, AUTHOR, BlogStatus.PUBLISHED);
            b.setThumbnailUrl("http://minio/blog-thumbnails/old.jpg");
            when(blogRepository.findById(1L)).thenReturn(Optional.of(b));
            when(storage.upload(eq(BlogServiceImpl.THUMBNAIL_BUCKET), anyString(), any(InputStream.class), anyLong(), eq("image/png")))
                    .thenReturn("http://minio/blog-thumbnails/new.png");
            stubSaveReturnsArgument();

            BlogDetailResponse res = blogService.uploadThumbnail(1L, AUTHOR,
                    new MockMultipartFile("image", "a.png", "image/png", new byte[]{1, 2, 3}));

            assertThat(res.thumbnailUrl()).isEqualTo("http://minio/blog-thumbnails/new.png");
            verify(storage).deleteByUrl(BlogServiceImpl.THUMBNAIL_BUCKET, "http://minio/blog-thumbnails/old.jpg");
        }
    }

    // ---- Danh sách ----
    @Nested
    class Listing {

        @Test
        void listPublished_keywordSearchesTitle() {
            when(blogRepository.findByStatusAndTitleContainingIgnoreCase(eq(BlogStatus.PUBLISHED), eq("vsl"), any(Pageable.class)))
                    .thenReturn(Page.empty());

            blogService.listPublished("  vsl ", OTHER, 0, 10);

            verify(blogRepository).findByStatusAndTitleContainingIgnoreCase(eq(BlogStatus.PUBLISHED), eq("vsl"), any(Pageable.class));
        }

        @Test
        void listPublished_guestNewestFirst_loggedInPrioritizesFollowed() {
            when(blogRepository.findByStatus(eq(BlogStatus.PUBLISHED), any(Pageable.class))).thenReturn(Page.empty());
            when(blogRepository.findPublishedPrioritizingFollowed(eq(OTHER), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(blog(1L, AUTHOR, BlogStatus.PUBLISHED))));

            blogService.listPublished(null, null, 0, 10);
            var page = blogService.listPublished("   ", OTHER, 0, 10);

            verify(blogRepository).findByStatus(eq(BlogStatus.PUBLISHED), any(Pageable.class));
            verify(blogRepository).findPublishedPrioritizingFollowed(eq(OTHER), any(Pageable.class));
            assertThat(page.getContent()).hasSize(1);
            assertThat(page.getContent().get(0).author().fullName()).isEqualTo("Tác Giả");
        }

        @Test
        void search_includesBlogsOfMatchedAuthors() {
            when(identityClient.search("tac gia", 0, 10)).thenReturn(com.vsl.common.response.PageResponse.of(
                    List.of(new UserSummary(AUTHOR, "author", "Tác Giả", null)), 0, 10, 1));
            when(blogRepository.searchPublished(eq("tac gia"), eq(List.of(AUTHOR)), any(Pageable.class)))
                    .thenReturn(Page.empty());

            var res = blogService.search(" tac gia ", null, 0, 10);

            assertThat(res.users().getContent()).extracting("userId").containsExactly(AUTHOR);
            verify(blogRepository).searchPublished(eq("tac gia"), eq(List.of(AUTHOR)), any(Pageable.class));
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

        @Test
        void listForAdmin_passesNullKeywordWhenBlank() {
            loginAs(ADMIN, "ADMIN");
            when(blogRepository.findForAdmin(eq(ADMIN), isNull(), isNull(), any(Pageable.class))).thenReturn(Page.empty());

            blogService.listForAdmin(ADMIN, null, "  ", 0, 10);

            verify(blogRepository).findForAdmin(eq(ADMIN), isNull(), isNull(), any(Pageable.class));
        }
    }
}
