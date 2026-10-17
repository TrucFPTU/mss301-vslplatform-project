package com.vsl.social.service;

import com.vsl.common.exception.AppException;
import com.vsl.common.exception.ErrorCode;
import com.vsl.common.response.PageResponse;
import com.vsl.social.dto.ReportDetailResponse;
import com.vsl.social.dto.ReportRequest;
import com.vsl.social.dto.ReportResponse;
import com.vsl.social.entity.Blog;
import com.vsl.social.entity.BlogReport;
import com.vsl.social.entity.BlogStatus;
import com.vsl.social.entity.ReportStatus;
import com.vsl.social.repository.BlogReportRepository;
import com.vsl.social.repository.BlogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/** Nghiệp vụ báo cáo bài viết & Admin duyệt/xử lý báo cáo vi phạm. */
@Service
@RequiredArgsConstructor
public class ReportService {

    private final BlogService blogService;
    private final BlogRepository blogRepository;
    private final BlogReportRepository blogReportRepository;

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

    // ---- Admin duyệt báo cáo vi phạm ----

    @Transactional(readOnly = true)
    public PageResponse<ReportResponse> listReports(ReportStatus status, int page, int size) {
        AccessControl.requireAdmin();
        Pageable pageable = Paging.newestFirst(page, size);
        Page<BlogReport> result = status != null
                ? blogReportRepository.findByStatus(status, pageable)
                : blogReportRepository.findAllWithBlog(pageable);
        return Paging.toResponse(result, ReportResponse::from);
    }

    @Transactional(readOnly = true)
    public ReportDetailResponse getReportDetail(Long reportId) {
        AccessControl.requireAdmin();
        BlogReport report = findReportOrThrow(reportId);
        return ReportDetailResponse.from(report);
    }

    /**
     * Admin duyệt xử lý vi phạm:
     * - Chuyển trạng thái báo cáo sang RESOLVED.
     * - Tự động ẩn bài viết bị tố cáo (chuyển sang HIDDEN).
     */
    @Transactional
    public ReportResponse resolve(Long reportId) {
        AccessControl.requireAdmin();
        BlogReport report = findReportOrThrow(reportId);
        if (report.getStatus() != ReportStatus.PENDING) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Báo cáo này đã được xử lý trước đó");
        }

        report.setStatus(ReportStatus.RESOLVED);
        Blog blog = report.getBlog();
        blog.setStatus(BlogStatus.HIDDEN);
        blogRepository.saveAndFlush(blog);

        BlogReport saved = blogReportRepository.saveAndFlush(report);
        return ReportResponse.from(saved);
    }

    /**
     * Admin bác bỏ báo cáo (bài viết hợp lệ, không vi phạm):
     * - Chuyển trạng thái báo cáo sang REJECTED.
     * - Bài viết được giữ nguyên trạng thái hiện tại.
     */
    @Transactional
    public ReportResponse reject(Long reportId) {
        AccessControl.requireAdmin();
        BlogReport report = findReportOrThrow(reportId);
        if (report.getStatus() != ReportStatus.PENDING) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Báo cáo này đã được xử lý trước đó");
        }

        report.setStatus(ReportStatus.REJECTED);
        BlogReport saved = blogReportRepository.saveAndFlush(report);
        return ReportResponse.from(saved);
    }

    private BlogReport findReportOrThrow(Long reportId) {
        return blogReportRepository.findWithBlogById(reportId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Không tìm thấy báo cáo vi phạm"));
    }
}

