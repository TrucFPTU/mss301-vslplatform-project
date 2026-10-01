package com.vsl.learning.service;

import com.vsl.common.exception.AppException;
import com.vsl.common.exception.ErrorCode;
import com.vsl.learning.client.AiServiceClient;
import com.vsl.learning.dto.ai.EvaluationResponse;
import com.vsl.learning.dto.response.PracticeStatsResponse;
import com.vsl.learning.dto.response.UserProgressResponse;
import com.vsl.learning.entity.LearningStatus;
import com.vsl.learning.entity.UserProgress;
import com.vsl.learning.entity.Vocabulary;
import com.vsl.learning.repository.AttemptHistoryRepository;
import com.vsl.learning.repository.UserProgressRepository;
import com.vsl.learning.repository.VocabularyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * Điều phối luồng luyện tập: nhận video -> nhờ ai-service chấm điểm -> tự lưu lịch sử,
 * cập nhật tiến trình và mở khoá thành tựu. Kèm thống kê / tiến trình của người học.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PracticeService {

    private final AiServiceClient aiServiceClient;
    private final AttemptService attemptService;
    private final VocabularyRepository vocabularyRepository;
    private final AttemptHistoryRepository attemptHistoryRepository;
    private final UserProgressRepository userProgressRepository;

    /** Không đặt @Transactional: tránh giữ kết nối DB trong lúc chờ ai-service suy luận. */
    public EvaluationResponse evaluate(Long userId, MultipartFile video, int expectedId,
                                       float startFrac, float endFrac) {
        validateVideo(video);
        validateWindow(startFrac, endFrac);

        Vocabulary vocabulary = vocabularyRepository.findFirstByExpectedId(expectedId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND,
                        "Không tìm thấy từ vựng ứng với expectedId=" + expectedId));

        List<Integer> validIds = vocabularyRepository.findExpectedIdsWithVideo();
        EvaluationResponse result = aiServiceClient.evaluate(video, expectedId, startFrac, endFrac, validIds);

        // Ghi lịch sử là phần phụ: lỗi DB không được làm mất kết quả chấm điểm của người dùng.
        try {
            boolean passed = EvaluationResponse.STATUS_CORRECT.equals(result.getStatus());
            attemptService.recordAttempt(userId, vocabulary, passed, result.getPredictedId(), result.getConfidence());
        } catch (Exception e) {
            log.warn("Không lưu được lịch sử luyện tập userId={}, expectedId={}: {}",
                    userId, expectedId, e.getMessage());
        }

        log.info("Evaluation done: userId={}, expectedId={}, status={}, confidence={}%, rank={}",
                userId, expectedId, result.getStatus(), result.getConfidence(), result.getRank());
        return result;
    }

    @Transactional(readOnly = true)
    public List<UserProgressResponse> getMyProgress(Long userId) {
        return userProgressRepository.findAllWithVocabularyByUserId(userId).stream()
                .map(PracticeService::toProgressResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PracticeStatsResponse getStats(Long userId) {
        long totalAttempts = attemptHistoryRepository.countByUserId(userId);
        long correctAttempts = attemptHistoryRepository.countByUserIdAndIsCorrect(userId, true);
        long learnedCount = userProgressRepository.countByUserIdAndLearningStatus(userId, LearningStatus.LEARNED);
        long totalVocabs = vocabularyRepository.count();

        double accuracyRate = totalAttempts > 0
                ? Math.round((double) correctAttempts / totalAttempts * 10000.0) / 100.0
                : 0.0;

        List<LocalDate> practiceDates = attemptHistoryRepository.findAttemptTimesByUserId(userId).stream()
                .map(LocalDateTime::toLocalDate)
                .distinct()
                .sorted(Comparator.reverseOrder())
                .toList();
        PracticeStreakCalculator.StreakResult streak =
                PracticeStreakCalculator.computeStreak(practiceDates, LocalDate.now());

        return PracticeStatsResponse.builder()
                .totalAttempts(totalAttempts)
                .correctAttempts(correctAttempts)
                .learnedCount(learnedCount)
                .totalVocabs(totalVocabs)
                .accuracyRate(accuracyRate)
                .proficiency(AchievementService.computeProficiency(learnedCount, totalVocabs))
                .currentStreak(streak.current())
                .longestStreak(streak.longest())
                .weekActivity(streak.week())
                .build();
    }

    private static void validateVideo(MultipartFile video) {
        if (video == null || video.isEmpty()) {
            throw new AppException(ErrorCode.VIDEO_EMPTY);
        }
        String contentType = video.getContentType();
        if (contentType == null || !contentType.startsWith("video/")) {
            throw new AppException(ErrorCode.VIDEO_INVALID_TYPE);
        }
    }

    private static void validateWindow(float startFrac, float endFrac) {
        if (startFrac < 0f || endFrac > 1f || startFrac >= endFrac) {
            throw new AppException(ErrorCode.VALIDATION_ERROR,
                    "startFrac/endFrac phải thoả 0 <= startFrac < endFrac <= 1");
        }
    }

    private static UserProgressResponse toProgressResponse(UserProgress progress) {
        Vocabulary vocabulary = progress.getVocabulary();
        return UserProgressResponse.builder()
                .vocabularyId(vocabulary.getId())
                .word(vocabulary.getWord())
                .categoryName(vocabulary.getCategory() != null ? vocabulary.getCategory().getName() : null)
                .learningStatus(progress.getLearningStatus())
                .lastAttemptedAt(progress.getLastAttemptedAt())
                .build();
    }
}
