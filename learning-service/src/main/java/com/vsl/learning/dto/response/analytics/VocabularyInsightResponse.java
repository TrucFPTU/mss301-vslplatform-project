package com.vsl.learning.dto.response.analytics;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@Schema(description = "Phân tích chuyên sâu về từ vựng (Top luyện tập, từ khó, từ ít học)")
public class VocabularyInsightResponse {

    @Schema(description = "Top từ vựng được luyện tập nhiều nhất")
    private final List<VocabularyStatItem> topPracticed;

    @Schema(description = "Top từ vựng có tỷ lệ làm sai cao nhất (từ khó)")
    private final List<VocabularyStatItem> mostDifficult;

    @Schema(description = "Top từ vựng ít được luyện tập nhất")
    private final List<VocabularyStatItem> leastPracticed;
}
