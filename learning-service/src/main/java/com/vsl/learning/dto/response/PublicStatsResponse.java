package com.vsl.learning.dto.response;

/**
 * Số liệu trang chủ - toàn bộ là số thật (bỏ "tỉ lệ hài lòng" hằng số của monolith).
 * totalStudents = số tài khoản học viên (role USER); null khi identity-service tạm không phản hồi.
 */
public record PublicStatsResponse(
        Long totalStudents,
        long totalVocabs,
        long totalCategories
) {
}
