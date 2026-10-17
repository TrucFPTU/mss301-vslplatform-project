package com.vsl.social.dto;

import lombok.Builder;

@Builder
public record SocialDashboardOverviewResponse(
        long totalBlogs,
        long publishedBlogs,
        long hiddenBlogs,
        long draftBlogs,
        long totalLikes,
        long totalComments,
        long totalShares,
        long pendingReports,
        long resolvedReports,
        long rejectedReports
) {
}
