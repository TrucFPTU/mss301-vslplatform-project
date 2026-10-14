package com.vsl.social.service;

import com.vsl.common.exception.ErrorCode;
import com.vsl.social.dto.ReportRequest;
import com.vsl.social.entity.Blog;
import com.vsl.social.entity.BlogReport;
import com.vsl.social.entity.BlogShare;
import com.vsl.social.entity.BlogStatus;
import com.vsl.social.entity.NotificationType;
import com.vsl.social.entity.ReportStatus;
import com.vsl.social.repository.BlogReportRepository;
import com.vsl.social.repository.BlogRepository;
import com.vsl.social.repository.BlogShareRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static com.vsl.social.service.TestSupport.AUTHOR;
import static com.vsl.social.service.TestSupport.OTHER;
import static com.vsl.social.service.TestSupport.assertAppError;
import static com.vsl.social.service.TestSupport.blog;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReportAndShareServiceTest {

    private final BlogService blogService = mock(BlogService.class);
    private final Blog blog = blog(1L, AUTHOR, BlogStatus.PUBLISHED);

    // ---- Report ----

    private final BlogReportRepository reportRepository = mock(BlogReportRepository.class);
    private final ReportService reportService = new ReportService(blogService, reportRepository);

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

    // ---- Share ----

    @Test
    void share_savesNotifiesAndReturnsNewCount() {
        BlogRepository blogRepository = mock(BlogRepository.class);
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
