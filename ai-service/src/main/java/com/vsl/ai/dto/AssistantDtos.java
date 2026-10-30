package com.vsl.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/** DTO cho trợ lý học tập (chat). */
public final class AssistantDtos {

    private AssistantDtos() {
    }

    // ---------- FE -> ai-service ----------

    /** role: "user" (người học) hoặc "assistant" (câu trả lời trước đó của trợ lý). */
    public record ChatMessage(
            @NotBlank @Pattern(regexp = "user|assistant") String role,
            @NotBlank @Size(max = 2000) String content) {
    }

    /** Lịch sử chat do FE giữ (backend không lưu); tin cuối phải là câu hỏi của người học. */
    public record ChatRequest(@NotEmpty @Size(max = 30) List<@Valid ChatMessage> messages) {
    }

    /**
     * Thẻ để người học bấm. type = VOCABULARY (id = vocabularyId) hoặc CATEGORY (id = categoryId).
     * Chỉ tạo từ id CÓ THẬT trong dữ liệu learning-service, không lấy từ chữ AI viết ra.
     */
    public record Card(String type, Long id, String title, String subtitle, String imageUrl, boolean practicable) {
    }

    public record ChatResponse(String reply, List<Card> cards) {
    }

    // ---------- learning-service -> ai-service (GET /api/practice/assistant-context) ----------

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LearningContext(int totalCategories, int totalVocabularies, long practicableVocabularies,
                                  Stats stats, List<CategoryItem> categories, List<VocabularyItem> vocabularies,
                                  List<Recommendation> recommendations) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Stats(long totalAttempts, long correctAttempts, long learnedCount, long totalVocabs,
                        double accuracyRate, int proficiency, int currentStreak, int longestStreak,
                        List<Boolean> weekActivity) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CategoryItem(Long id, String name, int vocabularyCount, int learnedCount, int learningCount) {
    }

    /** status: NEW / LEARNING / LEARNED. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record VocabularyItem(Long id, String word, Long categoryId, String categoryName, String imageUrl,
                                 boolean practicable, String status) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Recommendation(Long vocabularyId, String word, String reason) {
    }

    /** Envelope ApiResponse của learning-service (ApiResponse common-lib không deserialize được). */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RemoteResponse<T>(boolean success, String message, T data) {
    }
}
