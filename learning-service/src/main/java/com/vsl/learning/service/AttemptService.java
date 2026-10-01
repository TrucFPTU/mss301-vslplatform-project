package com.vsl.learning.service;

import com.vsl.common.response.PageResponse;
import com.vsl.learning.dto.response.AttemptResponse;
import com.vsl.learning.entity.AttemptHistory;
import com.vsl.learning.entity.Category;
import com.vsl.learning.entity.LearningStatus;
import com.vsl.learning.entity.UserProgress;
import com.vsl.learning.entity.Vocabulary;
import com.vsl.learning.repository.AttemptHistoryRepository;
import com.vsl.learning.repository.UserProgressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AttemptService {

    private static final int MAX_RECENT_LIMIT = 50;

    private final AttemptHistoryRepository attemptHistoryRepository;
    private final UserProgressRepository userProgressRepository;
    private final AchievementService achievementService;

    @Transactional(readOnly = true)
    public PageResponse<AttemptResponse> getMyAttempts(Long userId, int page, int size) {
        return Paging.toPageResponse(attemptHistoryRepository
                .findPageByUserId(userId, PageRequest.of(Paging.safePage(page), Paging.safeSize(size)))
                .map(AttemptService::toResponse));
    }

    @Transactional(readOnly = true)
    public List<AttemptResponse> getRecentAttempts(Long userId, int limit) {
        return attemptHistoryRepository
                .findPageByUserId(userId, PageRequest.of(0, Paging.clamp(limit, 1, MAX_RECENT_LIMIT)))
                .map(AttemptService::toResponse)
                .getContent();
    }

    /**
     * Ghi kết quả 1 lần luyện tập (do ai-service chấm): lưu AttemptHistory,
     * cập nhật UserProgress rồi kiểm tra mở khoá thành tựu — trong cùng 1 transaction.
     */
    @Transactional
    public void recordAttempt(Long userId, Vocabulary vocabulary, boolean passed, int predictedId, double confidence) {
        LocalDateTime now = LocalDateTime.now();
        attemptHistoryRepository.save(AttemptHistory.builder()
                .userId(userId)
                .vocabulary(vocabulary)
                .isCorrect(passed)
                .aiPredictedCode((long) predictedId)
                .confidence(confidence)
                .attemptedAt(now)
                .build());

        updateUserProgress(userId, vocabulary, passed, now);
        achievementService.checkAndUnlock(userId);
    }

    /**
     * Trạng thái chỉ đi 1 chiều LEARNING -> LEARNED: lần pass đầu tiên chuyển thành LEARNED
     * và các lần fail sau đó KHÔNG kéo ngược lại.
     */
    private void updateUserProgress(Long userId, Vocabulary vocabulary, boolean passed, LocalDateTime now) {
        UserProgress progress = userProgressRepository
                .findByUserIdAndVocabulary_Id(userId, vocabulary.getId())
                .orElseGet(() -> UserProgress.builder()
                        .userId(userId)
                        .vocabulary(vocabulary)
                        .learningStatus(LearningStatus.LEARNING)
                        .build());

        if (passed) {
            progress.setLearningStatus(LearningStatus.LEARNED);
        } else if (progress.getLearningStatus() == null) {
            progress.setLearningStatus(LearningStatus.LEARNING);
        }
        progress.setLastAttemptedAt(now);
        userProgressRepository.save(progress);
    }

    private static AttemptResponse toResponse(AttemptHistory attempt) {
        Vocabulary vocabulary = attempt.getVocabulary();
        Category category = vocabulary != null ? vocabulary.getCategory() : null;

        return AttemptResponse.builder()
                .attemptId(attempt.getId())
                .vocabularyId(vocabulary != null ? vocabulary.getId() : null)
                .word(vocabulary != null ? vocabulary.getWord() : null)
                .categoryId(category != null ? category.getId() : null)
                .categoryName(category != null ? category.getName() : null)
                .expectedId(vocabulary != null ? vocabulary.getExpectedId() : null)
                .isCorrect(attempt.getIsCorrect())
                .aiPredictedCode(attempt.getAiPredictedCode())
                .confidence(attempt.getConfidence())
                .attemptedAt(attempt.getAttemptedAt())
                .build();
    }
}
