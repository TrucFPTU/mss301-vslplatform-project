package com.vsl.social.dto;

/** Số liệu blog công khai (chỉ tính bài PUBLISHED) - learning-service gọi để tổng hợp thống kê. */
public record PublicBlogStatsResponse(
        long totalPublishedBlogs
) {
}
