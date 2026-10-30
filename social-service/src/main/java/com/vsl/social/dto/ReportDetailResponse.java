package com.vsl.social.dto;

import com.vsl.social.entity.BlogStatus;
import com.vsl.social.entity.ReportStatus;

import java.time.Instant;

/** DTO chi tiết báo cáo vi phạm kèm đầy đủ thông tin bài bị tố cáo. */
public record ReportDetailResponse(
        Long id,
        Long blogId,
        String blogTitle,
        String blogContent,
        String blogThumbnailUrl,
        BlogStatus blogStatus,
        String deletionReason,
        UserBriefResponse author,
        UserBriefResponse reporter,
        String reason,
        ReportStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
