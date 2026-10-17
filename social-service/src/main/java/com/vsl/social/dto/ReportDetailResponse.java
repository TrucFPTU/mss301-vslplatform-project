package com.vsl.social.dto;

import com.vsl.social.entity.BlogReport;
import com.vsl.social.entity.BlogStatus;
import com.vsl.social.entity.ReportStatus;

import java.time.Instant;

/** DTO chi tiết báo cáo vi phạm kèm thông tin đầy đủ của bài viết bị tố cáo. */
public record ReportDetailResponse(
        Long id,
        Long blogId,
        String blogTitle,
        String blogContent,
        BlogStatus blogStatus,
        Long authorId,
        Long reporterId,
        String reason,
        ReportStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public static ReportDetailResponse from(BlogReport r) {
        return new ReportDetailResponse(
                r.getId(),
                r.getBlog().getId(),
                r.getBlog().getTitle(),
                r.getBlog().getContent(),
                r.getBlog().getStatus(),
                r.getBlog().getAuthorId(),
                r.getReporterId(),
                r.getReason(),
                r.getStatus(),
                r.getCreatedAt(),
                r.getUpdatedAt()
        );
    }
}
