package com.vsl.social.repository;

import com.vsl.social.entity.BlogReport;
import com.vsl.social.entity.ReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BlogReportRepository extends JpaRepository<BlogReport, Long> {

    boolean existsByBlogIdAndReporterIdAndStatus(Long blogId, Long reporterId, ReportStatus status);
}
