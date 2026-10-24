package com.vsl.social.service.impl;

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
import com.vsl.social.service.BlogService;
import com.vsl.social.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

import org.springframework.data.domain.Page;

/** Người dùng báo cáo bài viết. Phần admin duyệt báo cáo: chờ thống nhất với người phụ trách Admin. */
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final BlogService blogService;
    private final BlogReportRepository blogReportRepository;

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
        return Paging.toResponse(result, ReportResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public ReportDetailResponse getReportDetail(Long id) {
        return ReportDetailResponse.from(getReport(id));
    }

    @Override
    @Transactional
    public ReportResponse resolve(Long id) {
        BlogReport report = getReport(id);
        report.setStatus(ReportStatus.RESOLVED);
        report.getBlog().setStatus(BlogStatus.HIDDEN);
        return ReportResponse.from(report);
    }

    @Override
    @Transactional
    public ReportResponse reject(Long id) {
        BlogReport report = getReport(id);
        report.setStatus(ReportStatus.REJECTED);
        return ReportResponse.from(report);
    }

    private BlogReport getReport(Long id) {
        return blogReportRepository.findWithBlogById(id)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
    }
}
