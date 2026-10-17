package com.vsl.social.dto;

import com.vsl.social.entity.BlogReport;
import com.vsl.social.entity.ReportStatus;

import java.time.Instant;

/** DTO tóm tắt báo cáo vi phạm cho danh sách quản trị. */
public record ReportResponse(
        Long id,
        Long blogId,
        String blogTitle,
        Long reporterId,
        String reason,
        ReportStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public static ReportResponse from(BlogReport r) {
        return new ReportResponse(
                r.getId(),
                r.getBlog().getId(),
                r.getBlog().getTitle(),
                r.getReporterId(),
                r.getReason(),
                r.getStatus(),
                r.getCreatedAt(),
                r.getUpdatedAt()
        );
    }
}
