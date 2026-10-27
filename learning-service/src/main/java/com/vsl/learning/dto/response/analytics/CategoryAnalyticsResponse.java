package com.vsl.learning.dto.response.analytics;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "Thống kê tiến độ học tập và độ chính xác theo từng danh mục")
public class CategoryAnalyticsResponse {

    @Schema(description = "ID danh mục", example = "1")
    private final Long categoryId;

    @Schema(description = "Tên danh mục", example = "Chào hỏi cơ bản")
    private final String categoryName;

    @Schema(description = "Tổng số từ vựng trong danh mục", example = "15")
    private final long totalVocabularies;

    @Schema(description = "Tổng số lượt luyện tập của các từ trong danh mục", example = "240")
    private final long totalAttempts;

    @Schema(description = "Số lượt từ vựng được học viên hoàn thành (LEARNED)", example = "65")
    private final long learnedCount;

    @Schema(description = "Tỷ lệ làm đúng trong danh mục (%)", example = "84.2")
    private final double accuracyRate;
}
