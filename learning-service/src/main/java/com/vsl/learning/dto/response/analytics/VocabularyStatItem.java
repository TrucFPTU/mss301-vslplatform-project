package com.vsl.learning.dto.response.analytics;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "Thống kê chi tiết cho một từ vựng")
public class VocabularyStatItem {

    @Schema(description = "ID từ vựng", example = "1")
    private final Long vocabularyId;

    @Schema(description = "Từ vựng / Ký hiệu", example = "Xin chào")
    private final String word;

    @Schema(description = "Tên danh mục", example = "Giao tiếp hàng ngày")
    private final String categoryName;

    @Schema(description = "Số lượt luyện tập", example = "85")
    private final long attemptCount;

    @Schema(description = "Số lượt làm đúng", example = "70")
    private final long correctCount;

    @Schema(description = "Số lượt làm sai", example = "15")
    private final long incorrectCount;

    @Schema(description = "Tỷ lệ làm đúng (%)", example = "82.4")
    private final double accuracyRate;
}
