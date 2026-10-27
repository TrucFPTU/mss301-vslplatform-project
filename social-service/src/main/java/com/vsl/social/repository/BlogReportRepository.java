package com.vsl.social.repository;

import com.vsl.social.entity.BlogReport;
import com.vsl.social.entity.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.List;

public interface BlogReportRepository extends JpaRepository<BlogReport, Long> {

    boolean existsByBlogIdAndReporterIdAndStatus(Long blogId, Long reporterId, ReportStatus status);

    @EntityGraph(attributePaths = {"blog"})
    Page<BlogReport> findByStatus(ReportStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"blog"})
    @Query("SELECT r FROM BlogReport r")
    Page<BlogReport> findAllWithBlog(Pageable pageable);

    @EntityGraph(attributePaths = {"blog"})
    Optional<BlogReport> findWithBlogById(Long id);

    long countByStatus(ReportStatus status);

    boolean existsByBlogIdAndStatus(Long blogId, ReportStatus status);

    List<BlogReport> findByBlogIdAndStatus(Long blogId, ReportStatus status);
}

