package com.vsl.learning.dto.response.analytics;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "Thống kê tổng quan hệ thống học tập dành cho Dashboard Admin")
public class DashboardOverviewResponse {

    @Schema(description = "Tổng số từ vựng trong hệ thống", example = "120")
    private final long totalVocabularies;

    @Schema(description = "Tổng số danh mục bài học", example = "8")
    private final long totalCategories;

    @Schema(description = "Tổng số lượt luyện tập của toàn bộ người dùng", example = "1540")
    private final long totalAttempts;

    @Schema(description = "Tổng số học viên đã tham gia luyện tập", example = "85")
    private final long totalLearners;

    @Schema(description = "Tổng số lượt từ vựng đã được học viên làm chủ (trạng thái LEARNED)", example = "320")
    private final long totalLearnedWords;

    @Schema(description = "Tỷ lệ làm đúng trung bình toàn hệ thống (%)", example = "78.5")
    private final double overallAccuracyRate;

    @Schema(description = "Độ tin cậy trung bình của AI model khi đánh giá (0.0 - 1.0)", example = "0.86")
    private final double averageConfidence;

    @Schema(description = "Số lượt luyện tập được đánh giá đúng", example = "1210")
    private final long correctAttempts;

    @Schema(description = "Số lượt luyện tập được đánh giá sai", example = "330")
    private final long incorrectAttempts;
}
