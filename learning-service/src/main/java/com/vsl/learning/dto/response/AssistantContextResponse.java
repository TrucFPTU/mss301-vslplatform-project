package com.vsl.learning.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Dữ liệu cho trợ lý học tập (ai-service) trả lời người học: tổng quan hệ thống, tiến độ của CHÍNH người hỏi
 * và gợi ý bài tiếp theo. Gợi ý do learning tính (quy tắc cố định), AI chỉ diễn đạt lại.
 */
@Getter
@Builder
public class AssistantContextResponse {

    private final int totalCategories;
    private final int totalVocabularies;
    /** Số từ luyện được (có class AI + video mẫu) - mẫu số của "đã thuộc x/y". */
    private final long practicableVocabularies;
    private final PracticeStatsResponse stats;
    private final List<CategoryItem> categories;
    private final List<VocabularyItem> vocabularies;
    private final List<Recommendation> recommendations;

    /** status: NEW (chưa luyện) / LEARNING (đã luyện, chưa đúng) / LEARNED. */
    public record VocabularyItem(Long id, String word, Long categoryId, String categoryName, String imageUrl,
                                 boolean practicable, String status) {
    }

    public record CategoryItem(Long id, String name, int vocabularyCount, int learnedCount, int learningCount) {
    }

    public record Recommendation(Long vocabularyId, String word, String reason) {
    }
}
