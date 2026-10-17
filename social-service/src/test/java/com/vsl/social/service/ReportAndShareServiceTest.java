package com.vsl.social.service;

import com.vsl.common.exception.ErrorCode;
import com.vsl.common.response.PageResponse;
import com.vsl.social.dto.ReportDetailResponse;
import com.vsl.social.dto.ReportRequest;
import com.vsl.social.dto.ReportResponse;
import com.vsl.social.entity.Blog;
import com.vsl.social.entity.BlogReport;
import com.vsl.social.entity.BlogShare;
import com.vsl.social.entity.BlogStatus;
import com.vsl.social.entity.NotificationType;
import com.vsl.social.entity.ReportStatus;
import com.vsl.social.repository.BlogReportRepository;
import com.vsl.social.repository.BlogRepository;
import com.vsl.social.repository.BlogShareRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static com.vsl.social.service.TestSupport.ADMIN;
import static com.vsl.social.service.TestSupport.AUTHOR;
import static com.vsl.social.service.TestSupport.OTHER;
import static com.vsl.social.service.TestSupport.assertAppError;
import static com.vsl.social.service.TestSupport.blog;
import static com.vsl.social.service.TestSupport.loginAs;
import static com.vsl.social.service.TestSupport.logout;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReportAndShareServiceTest {

    private final BlogService blogService = mock(BlogService.class);
    private final BlogRepository blogRepository = mock(BlogRepository.class);
    private final BlogReportRepository reportRepository = mock(BlogReportRepository.class);
    private final ReportService reportService = new ReportService(blogService, blogRepository, reportRepository);

    private final Blog blog = blog(1L, AUTHOR, BlogStatus.PUBLISHED);

    @AfterEach
    void tearDown() {
        logout();
    }

    // ---- User Report ----

    @Nested
    @DisplayName("User Report Tests")
    class UserReportTests {

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
    }

    // ---- Admin Report Review ----

    @Nested
    @DisplayName("Admin Report Review Tests")
    class AdminReportTests {

        private BlogReport createReport(Long id, ReportStatus status) {
            BlogReport r = BlogReport.builder()
                    .blog(blog)
                    .reporterId(OTHER)
                    .reason("Vi phạm tiêu chuẩn cộng đồng")
                    .status(status)
                    .build();
            r.setId(id);
            r.setCreatedAt(Instant.now());
            r.setUpdatedAt(Instant.now());
            return r;
        }

        @Test
        @DisplayName("listReports: Người dùng thường bị từ chối FORBIDDEN")
        void listReports_NonAdmin_Forbidden() {
            loginAs(OTHER, "USER");
            assertAppError(() -> reportService.listReports(null, 0, 10), ErrorCode.FORBIDDEN);
        }

        @Test
        @DisplayName("listReports: Admin lấy danh sách lọc theo status thành công")
        void listReports_Admin_WithStatus_Success() {
            loginAs(ADMIN, "ADMIN");
            BlogReport r = createReport(10L, ReportStatus.PENDING);
            Page<BlogReport> page = new PageImpl<>(List.of(r));

            when(reportRepository.findByStatus(eq(ReportStatus.PENDING), any(Pageable.class))).thenReturn(page);

            PageResponse<ReportResponse> result = reportService.listReports(ReportStatus.PENDING, 0, 10);
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).id()).isEqualTo(10L);
            assertThat(result.getContent().get(0).status()).isEqualTo(ReportStatus.PENDING);
        }

        @Test
        @DisplayName("listReports: Admin lấy danh sách không truyền status (lấy tất cả)")
        void listReports_Admin_AllStatuses_Success() {
            loginAs(ADMIN, "ADMIN");
            BlogReport r1 = createReport(10L, ReportStatus.PENDING);
            BlogReport r2 = createReport(11L, ReportStatus.RESOLVED);
            Page<BlogReport> page = new PageImpl<>(List.of(r1, r2));

            when(reportRepository.findAllWithBlog(any(Pageable.class))).thenReturn(page);

            PageResponse<ReportResponse> result = reportService.listReports(null, 0, 10);
            assertThat(result.getContent()).hasSize(2);
        }

        @Test
        @DisplayName("getReportDetail: Admin xem chi tiết báo cáo và bài viết")
        void getReportDetail_Success() {
            loginAs(ADMIN, "ADMIN");
            BlogReport r = createReport(10L, ReportStatus.PENDING);
            when(reportRepository.findWithBlogById(10L)).thenReturn(Optional.of(r));

            ReportDetailResponse detail = reportService.getReportDetail(10L);
            assertThat(detail.id()).isEqualTo(10L);
            assertThat(detail.blogId()).isEqualTo(1L);
            assertThat(detail.blogTitle()).isEqualTo(blog.getTitle());
            assertThat(detail.reason()).isEqualTo("Vi phạm tiêu chuẩn cộng đồng");
        }

        @Test
        @DisplayName("resolve: Admin duyệt báo cáo -> chuyển RESOLVED và ẩn bài viết (HIDDEN)")
        void resolve_Success() {
            loginAs(ADMIN, "ADMIN");
            BlogReport r = createReport(10L, ReportStatus.PENDING);
            when(reportRepository.findWithBlogById(10L)).thenReturn(Optional.of(r));
            when(reportRepository.saveAndFlush(any(BlogReport.class))).thenAnswer(inv -> inv.getArgument(0));

            ReportResponse response = reportService.resolve(10L);

            assertThat(response.status()).isEqualTo(ReportStatus.RESOLVED);
            assertThat(blog.getStatus()).isEqualTo(BlogStatus.HIDDEN);
            verify(blogRepository).saveAndFlush(blog);
            verify(reportRepository).saveAndFlush(r);
        }

        @Test
        @DisplayName("resolve: Báo cáo đã được xử lý trước đó sẽ bị từ chối")
        void resolve_AlreadyProcessed_Throws() {
            loginAs(ADMIN, "ADMIN");
            BlogReport r = createReport(10L, ReportStatus.RESOLVED);
            when(reportRepository.findWithBlogById(10L)).thenReturn(Optional.of(r));

            assertAppError(() -> reportService.resolve(10L), ErrorCode.VALIDATION_ERROR);
            verify(blogRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("reject: Admin bác bỏ báo cáo -> chuyển REJECTED và giữ nguyên bài viết")
        void reject_Success() {
            loginAs(ADMIN, "ADMIN");
            BlogReport r = createReport(10L, ReportStatus.PENDING);
            when(reportRepository.findWithBlogById(10L)).thenReturn(Optional.of(r));
            when(reportRepository.saveAndFlush(any(BlogReport.class))).thenAnswer(inv -> inv.getArgument(0));

            ReportResponse response = reportService.reject(10L);

            assertThat(response.status()).isEqualTo(ReportStatus.REJECTED);
            assertThat(blog.getStatus()).isEqualTo(BlogStatus.PUBLISHED); // Giữ nguyên
            verify(blogRepository, never()).saveAndFlush(any());
            verify(reportRepository).saveAndFlush(r);
        }
    }

    // ---- Share ----

    @Nested
    @DisplayName("Share Tests")
    class ShareTests {

        @Test
        void share_savesNotifiesAndReturnsNewCount() {
            BlogShareRepository shareRepository = mock(BlogShareRepository.class);
            NotificationService notificationService = mock(NotificationService.class);
            ShareService shareService = new ShareService(blogService, blogRepository, shareRepository, notificationService);

            Blog after = blog(1L, AUTHOR, BlogStatus.PUBLISHED);
            after.setShareCount(5);
            when(blogService.getVisibleBlog(1L, OTHER)).thenReturn(blog);
            when(blogService.getBlog(1L)).thenReturn(after);

            assertThat(shareService.share(1L, OTHER)).isEqualTo(5);
            verify(shareRepository).save(any(BlogShare.class));
            verify(blogRepository).addShareCount(1L, 1);
            verify(notificationService).notify(AUTHOR, OTHER, NotificationType.SHARE, 1L);
        }
    }
}
