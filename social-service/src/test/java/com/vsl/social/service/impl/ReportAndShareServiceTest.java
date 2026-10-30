package com.vsl.social.service.impl;

import com.vsl.common.exception.ErrorCode;
import com.vsl.social.dto.ReportRequest;
import com.vsl.social.dto.ReportResponse;
import com.vsl.social.dto.ShareRequest;
import com.vsl.social.dto.ShareResponse;
import com.vsl.social.entity.Blog;
import com.vsl.social.entity.BlogReport;
import com.vsl.social.entity.BlogShare;
import com.vsl.social.entity.BlogStatus;
import com.vsl.social.entity.NotificationType;
import com.vsl.social.entity.ReportStatus;
import com.vsl.social.entity.ShareType;
import com.vsl.social.repository.BlogReportRepository;
import com.vsl.social.repository.BlogRepository;
import com.vsl.social.repository.BlogShareRepository;
import com.vsl.social.repository.UserFollowRepository;
import com.vsl.social.service.BlogService;
import com.vsl.social.service.NotificationService;
import com.vsl.social.service.ReportService;
import com.vsl.social.service.ShareService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static com.vsl.social.service.impl.TestSupport.ADMIN;
import static com.vsl.social.service.impl.TestSupport.AUTHOR;
import static com.vsl.social.service.impl.TestSupport.OTHER;
import static com.vsl.social.service.impl.TestSupport.assertAppError;
import static com.vsl.social.service.impl.TestSupport.blog;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReportAndShareServiceTest {

    private final BlogService blogService = mock(BlogService.class);
    private final SocialViews views = mock(SocialViews.class);
    private final Blog blog = blog(1L, AUTHOR, BlogStatus.PUBLISHED);

    @Nested
    class Reports {

        private final BlogReportRepository reportRepository = mock(BlogReportRepository.class);
        private final ReportService reportService = new ReportServiceImpl(blogService, reportRepository, views);

        @Test
        void report_ownBlog_rejected() {
            when(blogService.getVisibleBlog(1L, AUTHOR)).thenReturn(blog);

            assertAppError(() -> reportService.report(1L, AUTHOR, new ReportRequest("x")), ErrorCode.REPORT_OWN_BLOG);
            verify(reportRepository, never()).save(any());
        }

        @Test
        void report_duplicatePending_rejected() {
            when(blogService.getVisibleBlog(1L, OTHER)).thenReturn(blog);
            when(reportRepository.existsByBlogIdAndReporterIdAndStatus(1L, OTHER, ReportStatus.PENDING)).thenReturn(true);

            assertAppError(() -> reportService.report(1L, OTHER, new ReportRequest("spam")), ErrorCode.REPORT_ALREADY_PENDING);
            verify(reportRepository, never()).save(any());
        }

        @Test
        void report_savedAsPendingWithTrimmedReason() {
            when(blogService.getVisibleBlog(1L, OTHER)).thenReturn(blog);

            reportService.report(1L, OTHER, new ReportRequest("  spam  "));

            ArgumentCaptor<BlogReport> saved = ArgumentCaptor.forClass(BlogReport.class);
            verify(reportRepository).save(saved.capture());
            assertThat(saved.getValue().getStatus()).isEqualTo(ReportStatus.PENDING);
            assertThat(saved.getValue().getReason()).isEqualTo("spam");
            assertThat(saved.getValue().getReporterId()).isEqualTo(OTHER);
        }

        @Test
        void resolve_hidesBlogWithReason() {
            BlogReport r = BlogReport.builder().blog(blog).reporterId(OTHER).reason("x").status(ReportStatus.PENDING).build();
            when(reportRepository.findWithBlogById(7L)).thenReturn(Optional.of(r));
            when(views.reports(List.of(r))).thenReturn(List.of(mock(ReportResponse.class)));

            reportService.resolve(7L, ADMIN, "Spam");

            verify(blogService).hide(1L, ADMIN, "Spam");
            assertThat(r.getStatus()).isEqualTo(ReportStatus.RESOLVED);
        }

        @Test
        void reject_keepsBlog() {
            BlogReport r = BlogReport.builder().blog(blog).reporterId(OTHER).reason("x").status(ReportStatus.PENDING).build();
            when(reportRepository.findWithBlogById(7L)).thenReturn(Optional.of(r));
            when(views.reports(List.of(r))).thenReturn(List.of(mock(ReportResponse.class)));

            reportService.reject(7L);

            assertThat(r.getStatus()).isEqualTo(ReportStatus.REJECTED);
            verify(blogService, never()).hide(any(), any(), any());
        }

        @Test
        void resolve_alreadyHandled_rejected() {
            BlogReport r = BlogReport.builder().blog(blog).reporterId(OTHER).reason("x").status(ReportStatus.REJECTED).build();
            when(reportRepository.findWithBlogById(7L)).thenReturn(Optional.of(r));

            assertAppError(() -> reportService.resolve(7L, ADMIN, "x"), ErrorCode.VALIDATION_ERROR);
            verify(blogService, never()).hide(any(), any(), any());
        }
    }

    @Nested
    class Shares {

        private final BlogRepository blogRepository = mock(BlogRepository.class);
        private final BlogShareRepository shareRepository = mock(BlogShareRepository.class);
        private final UserFollowRepository followRepository = mock(UserFollowRepository.class);
        private final NotificationService notificationService = mock(NotificationService.class);
        private final ShareService shareService = new ShareServiceImpl(blogService, blogRepository, shareRepository,
                followRepository, notificationService, views, "https://vsl.test/");

        private Blog after() {
            Blog after = blog(1L, AUTHOR, BlogStatus.PUBLISHED);
            after.setShareCount(5);
            return after;
        }

        @Test
        void copyUrl_savesNotifiesAndReturnsCountAndBlogUrl() {
            when(blogService.getVisibleBlog(1L, OTHER)).thenReturn(blog);
            when(blogService.getBlog(1L)).thenReturn(after());

            ShareResponse res = shareService.share(1L, OTHER, null);

            assertThat(res.shareCount()).isEqualTo(5);
            assertThat(res.blogUrl()).isEqualTo("https://vsl.test/blogs/1");
            assertThat(res.shareType()).isEqualTo(ShareType.COPY_URL);
            verify(shareRepository).save(any(BlogShare.class));
            verify(blogRepository).addShareCount(1L, 1);
            verify(notificationService).notify(AUTHOR, OTHER, NotificationType.SHARE, 1L);
        }

        @Test
        void draftCannotBeShared() {
            when(blogService.getVisibleBlog(1L, AUTHOR)).thenReturn(blog(1L, AUTHOR, BlogStatus.DRAFT));

            assertAppError(() -> shareService.share(1L, AUTHOR, null), ErrorCode.VALIDATION_ERROR);
            verify(shareRepository, never()).save(any());
        }

        @Test
        void profileToNonFriend_rejected() {
            when(blogService.getVisibleBlog(1L, OTHER)).thenReturn(blog);
            when(followRepository.existsByFollowerIdAndFolloweeId(OTHER, 3L)).thenReturn(true);
            when(followRepository.existsByFollowerIdAndFolloweeId(3L, OTHER)).thenReturn(false);

            assertAppError(() -> shareService.share(1L, OTHER, new ShareRequest(ShareType.PROFILE, 3L)), ErrorCode.NOT_FRIEND);
            verify(shareRepository, never()).save(any());
        }

        @Test
        void profileToSelf_rejected() {
            when(blogService.getVisibleBlog(1L, OTHER)).thenReturn(blog);

            assertAppError(() -> shareService.share(1L, OTHER, new ShareRequest(ShareType.PROFILE, OTHER)),
                    ErrorCode.CANNOT_SHARE_TO_SELF);
        }

        @Test
        void profileToFriend_notifiesRecipient() {
            when(blogService.getVisibleBlog(1L, OTHER)).thenReturn(blog);
            when(blogService.getBlog(1L)).thenReturn(after());
            when(followRepository.existsByFollowerIdAndFolloweeId(OTHER, 3L)).thenReturn(true);
            when(followRepository.existsByFollowerIdAndFolloweeId(3L, OTHER)).thenReturn(true);

            shareService.share(1L, OTHER, new ShareRequest(ShareType.PROFILE, 3L));

            ArgumentCaptor<BlogShare> saved = ArgumentCaptor.forClass(BlogShare.class);
            verify(shareRepository).save(saved.capture());
            assertThat(saved.getValue().getRecipientId()).isEqualTo(3L);
            verify(notificationService).notify(3L, OTHER, NotificationType.SHARE, 1L);
        }
    }
}
