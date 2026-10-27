package com.vsl.identity.dto;

/** Số liệu người dùng công khai (landing page / dashboard tổng hợp ở learning-service). */
public record PublicUserStatsResponse(
        long totalUsers,
        long totalStudents
) {
}
