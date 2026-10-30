package com.vsl.social.service.impl;

import com.vsl.common.exception.AppException;
import com.vsl.common.exception.ErrorCode;
import com.vsl.common.response.PageResponse;
import com.vsl.social.dto.ReportDetailResponse;
import com.vsl.social.dto.ReportRequest;
import com.vsl.social.dto.ReportResponse;
import com.vsl.social.entity.Blog;
import com.vsl.social.entity.BlogReport;
import com.vsl.social.entity.ReportStatus;
import com.vsl.social.repository.BlogReportRepository;
import com.vsl.social.service.BlogService;
import com.vsl.social.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * Báo cáo vi phạm. Sửa so với bản trước: "resolve" ẩn bài nhưng KHÔNG có lý do và để các tố cáo khác của
 * cùng bài treo PENDING; giờ resolve = gỡ bài kèm lý do + đóng mọi tố cáo đang chờ (như removeBlogByAdmin
 * của monolith), reject = chỉ đóng tố cáo đó.
 */
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final BlogService blogService;
    private final BlogReportRepository blogReportRepository;
    private final SocialViews views;

    @Override
    @Transactional
    public void report(Long blogId, Long userId, ReportRequest req) {
        Blog blog = blogService.getVisibleBlog(blogId, userId);
        if (Objects.equals(blog.getAuthorId(), userId)) {
            throw new AppException(ErrorCode.REPORT_OWN_BLOG);
        }
        if (blogReportRepository.existsByBlogIdAndReporterIdAndStatus(blogId, userId, ReportStatus.PENDING)) {
            throw new AppException(ErrorCode.REPORT_ALREADY_PENDING);
        }
        blogReportRepository.save(BlogReport.builder()
                .blog(blog)
                .reporterId(userId)
                .reason(req.reason().trim())
                .status(ReportStatus.PENDING)
                .build());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReportResponse> listReports(ReportStatus status, int page, int size) {
        Page<BlogReport> result = status == null
                ? blogReportRepository.findAllWithBlog(Paging.newestFirst(page, size))
                : blogReportRepository.findByStatus(status, Paging.newestFirst(page, size));
        return SocialViews.page(result, views::reports);
    }

    @Override
    @Transactional(readOnly = true)
    public ReportDetailResponse getReportDetail(Long id) {
        return views.reportDetail(getReport(id));
    }

    @Override
    @Transactional(readOnly = true)
    public long countPending() {
        return blogReportRepository.countByStatus(ReportStatus.PENDING);
    }

    @Override
    @Transactional
    public ReportResponse resolve(Long id, Long adminId, String reason) {
        BlogReport report = getPendingReport(id);
        // hide(): HIDDEN + lý do + chuyển MỌI tố cáo PENDING của bài (kể cả tố cáo này) sang RESOLVED.
        blogService.hide(report.getBlog().getId(), adminId, reason);
        report.setStatus(ReportStatus.RESOLVED);
        return views.reports(List.of(report)).get(0);
    }

    @Override
    @Transactional
    public ReportResponse reject(Long id) {
        BlogReport report = getPendingReport(id);
        report.setStatus(ReportStatus.REJECTED);
        return views.reports(List.of(report)).get(0);
    }

    private BlogReport getPendingReport(Long id) {
        BlogReport report = getReport(id);
        if (report.getStatus() != ReportStatus.PENDING) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Báo cáo này đã được xử lý");
        }
        return report;
    }

    private BlogReport getReport(Long id) {
        return blogReportRepository.findWithBlogById(id)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Không tìm thấy báo cáo"));
    }
}
