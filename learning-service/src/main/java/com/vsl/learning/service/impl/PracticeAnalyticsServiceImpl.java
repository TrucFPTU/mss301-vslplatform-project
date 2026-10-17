package com.vsl.learning.service.impl;

import com.vsl.learning.dto.response.analytics.*;
import com.vsl.learning.entity.LearningStatus;
import com.vsl.learning.repository.AttemptHistoryRepository;
import com.vsl.learning.repository.CategoryRepository;
import com.vsl.learning.repository.UserProgressRepository;
import com.vsl.learning.repository.VocabularyRepository;
import com.vsl.learning.repository.projection.AttemptTrendProjection;
import com.vsl.learning.repository.projection.CategoryLearnedCountProjection;
import com.vsl.learning.repository.projection.CategoryPracticeProjection;
import com.vsl.learning.repository.projection.VocabularyPracticeProjection;
import com.vsl.learning.service.PracticeAnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PracticeAnalyticsServiceImpl implements PracticeAnalyticsService {

    private final AttemptHistoryRepository attemptHistoryRepository;
    private final VocabularyRepository vocabularyRepository;
    private final CategoryRepository categoryRepository;
    private final UserProgressRepository userProgressRepository;

    @Override
    public DashboardOverviewResponse getOverview() {
        long totalVocabularies = vocabularyRepository.count();
        long totalCategories = categoryRepository.count();
        long totalAttempts = attemptHistoryRepository.count();
        long correctAttempts = attemptHistoryRepository.countByIsCorrect(true);
        long incorrectAttempts = attemptHistoryRepository.countByIsCorrect(false);
        long totalLearners = attemptHistoryRepository.countDistinctLearners();
        long totalLearnedWords = userProgressRepository.countByLearningStatus(LearningStatus.LEARNED);

        double overallAccuracyRate = totalAttempts > 0
                ? roundToOneDecimal((double) correctAttempts / totalAttempts * 100.0)
                : 0.0;

        Double avgConfidence = attemptHistoryRepository.getAverageConfidence();
        double averageConfidence = avgConfidence != null ? roundToTwoDecimals(avgConfidence) : 0.0;

        return DashboardOverviewResponse.builder()
                .totalVocabularies(totalVocabularies)
                .totalCategories(totalCategories)
                .totalAttempts(totalAttempts)
                .totalLearners(totalLearners)
                .totalLearnedWords(totalLearnedWords)
                .overallAccuracyRate(overallAccuracyRate)
                .averageConfidence(averageConfidence)
                .correctAttempts(correctAttempts)
                .incorrectAttempts(incorrectAttempts)
                .build();
    }

    @Override
    public List<DailyActivityResponse> getActivityTrends(int days) {
        int safeDays = Math.max(1, Math.min(days, 90));
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(safeDays - 1L);

        LocalDateTime from = startDate.atStartOfDay();
        LocalDateTime to = today.atTime(LocalTime.MAX);

        List<AttemptTrendProjection> attempts = attemptHistoryRepository.findAttemptsBetween(from, to);

        Map<LocalDate, List<AttemptTrendProjection>> groupedByDate = attempts.stream()
                .filter(a -> a.getAttemptedAt() != null)
                .collect(Collectors.groupingBy(a -> a.getAttemptedAt().toLocalDate()));

        List<DailyActivityResponse> result = new ArrayList<>(safeDays);
        for (LocalDate d = startDate; !d.isAfter(today); d = d.plusDays(1)) {
            List<AttemptTrendProjection> dayAttempts = groupedByDate.getOrDefault(d, Collections.emptyList());
            long dayTotal = dayAttempts.size();
            long dayCorrect = dayAttempts.stream()
                    .filter(a -> Boolean.TRUE.equals(a.getIsCorrect()))
                    .count();
            long dayIncorrect = dayTotal - dayCorrect;
            long activeLearners = dayAttempts.stream()
                    .map(AttemptTrendProjection::getUserId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .count();

            double accuracyRate = dayTotal > 0
                    ? roundToOneDecimal((double) dayCorrect / dayTotal * 100.0)
                    : 0.0;

            result.add(DailyActivityResponse.builder()
                    .date(d)
                    .totalAttempts(dayTotal)
                    .correctAttempts(dayCorrect)
                    .incorrectAttempts(dayIncorrect)
                    .activeLearners(activeLearners)
                    .accuracyRate(accuracyRate)
                    .build());
        }

        return result;
    }

    @Override
    public VocabularyInsightResponse getVocabularyInsights(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 50));
        PageRequest pageRequest = PageRequest.of(0, safeLimit);

        List<VocabularyStatItem> topPracticed = attemptHistoryRepository.findTopPracticedVocabularies(pageRequest)
                .stream()
                .map(this::mapToStatItem)
                .toList();

        List<VocabularyStatItem> mostDifficult = attemptHistoryRepository.findMostDifficultVocabularies(1L, pageRequest)
                .stream()
                .map(this::mapToStatItem)
                .toList();

        List<VocabularyStatItem> leastPracticed = vocabularyRepository.findLeastPracticedVocabularies(pageRequest)
                .stream()
                .map(this::mapToStatItem)
                .toList();

        return VocabularyInsightResponse.builder()
                .topPracticed(topPracticed)
                .mostDifficult(mostDifficult)
                .leastPracticed(leastPracticed)
                .build();
    }

    @Override
    public List<CategoryAnalyticsResponse> getCategoryAnalytics() {
        List<CategoryPracticeProjection> practiceStats = categoryRepository.getCategoryPracticeStats();
        List<CategoryLearnedCountProjection> learnedProjections = userProgressRepository.countLearnedPerCategory();

        Map<Long, Long> learnedMap = learnedProjections.stream()
                .filter(p -> p.getCategoryId() != null)
                .collect(Collectors.toMap(
                        CategoryLearnedCountProjection::getCategoryId,
                        p -> p.getLearnedCount() != null ? p.getLearnedCount() : 0L,
                        (v1, v2) -> v1
                ));

        return practiceStats.stream()
                .map(stat -> {
                    long totalAttempts = stat.getTotalAttempts() != null ? stat.getTotalAttempts() : 0L;
                    long correctAttempts = stat.getCorrectAttempts() != null ? stat.getCorrectAttempts() : 0L;
                    long learnedCount = learnedMap.getOrDefault(stat.getCategoryId(), 0L);
                    double accuracyRate = totalAttempts > 0
                            ? roundToOneDecimal((double) correctAttempts / totalAttempts * 100.0)
                            : 0.0;

                    return CategoryAnalyticsResponse.builder()
                            .categoryId(stat.getCategoryId())
                            .categoryName(stat.getCategoryName())
                            .totalVocabularies(stat.getTotalVocabularies() != null ? stat.getTotalVocabularies() : 0L)
                            .totalAttempts(totalAttempts)
                            .learnedCount(learnedCount)
                            .accuracyRate(accuracyRate)
                            .build();
                })
                .toList();
    }

    private VocabularyStatItem mapToStatItem(VocabularyPracticeProjection proj) {
        long attempts = proj.getAttemptCount() != null ? proj.getAttemptCount() : 0L;
        long correct = proj.getCorrectCount() != null ? proj.getCorrectCount() : 0L;
        long incorrect = Math.max(0L, attempts - correct);
        double accuracyRate = attempts > 0 ? roundToOneDecimal((double) correct / attempts * 100.0) : 0.0;

        return VocabularyStatItem.builder()
                .vocabularyId(proj.getVocabularyId())
                .word(proj.getWord())
                .categoryName(proj.getCategoryName())
                .attemptCount(attempts)
                .correctCount(correct)
                .incorrectCount(incorrect)
                .accuracyRate(accuracyRate)
                .build();
    }

    private double roundToOneDecimal(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private double roundToTwoDecimals(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
