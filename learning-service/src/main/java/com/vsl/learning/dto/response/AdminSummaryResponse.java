package com.vsl.learning.dto.response;

/**
 * 4 chỉ số tổng quan cho dashboard admin. totalStudents / totalBlogs lấy từ identity / social;
 * null khi service đó tạm không phản hồi.
 */
public record AdminSummaryResponse(
        Long totalStudents,
        long totalVisits,
        long totalVocabularies,
        Long totalBlogs
) {
}
