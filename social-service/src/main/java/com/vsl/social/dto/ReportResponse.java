package com.vsl.social.dto;

import com.vsl.social.entity.BlogStatus;
import com.vsl.social.entity.ReportStatus;

import java.time.Instant;

/** DTO tóm tắt báo cáo vi phạm cho danh sách quản trị. */
public record ReportResponse(
        Long id,
        Long blogId,
        String blogTitle,
        BlogStatus blogStatus,
        UserBriefResponse reporter,
        String reason,
        ReportStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
