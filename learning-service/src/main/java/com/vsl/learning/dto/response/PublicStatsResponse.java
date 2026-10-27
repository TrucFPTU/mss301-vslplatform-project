package com.vsl.learning.dto.response;

/**
 * Số liệu trang chủ (giữ đúng tên khóa JSON của monolith: totalUsers, totalVocabs, satisfactionRate).
 * totalUsers = null khi identity-service tạm không phản hồi.
 */
public record PublicStatsResponse(
        Long totalUsers,
        long totalVocabs,
        int satisfactionRate
) {
}
