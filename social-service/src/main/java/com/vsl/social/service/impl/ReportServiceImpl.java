package com.vsl.social.service.impl;

import com.vsl.common.exception.AppException;
import com.vsl.common.exception.ErrorCode;
import com.vsl.social.dto.ReportRequest;
import com.vsl.social.entity.Blog;
import com.vsl.social.entity.BlogReport;
import com.vsl.social.entity.ReportStatus;
import com.vsl.social.repository.BlogReportRepository;
import com.vsl.social.service.BlogService;
import com.vsl.social.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

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
}
