package com.vsl.learning.service;

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
import com.vsl.learning.service.impl.PracticeAnalyticsServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PracticeAnalyticsServiceTest {

    @Mock
    private AttemptHistoryRepository attemptHistoryRepository;

    @Mock
    private VocabularyRepository vocabularyRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserProgressRepository userProgressRepository;

    @InjectMocks
    private PracticeAnalyticsServiceImpl analyticsService;

    @Nested
    @DisplayName("getOverview() tests")
    class GetOverviewTests {

        @Test
        @DisplayName("Happy path: Tinh toan thong ke tong quan khi he thong co du lieu")
        void getOverview_SuccessWithData() {
            when(vocabularyRepository.count()).thenReturn(50L);
            when(categoryRepository.count()).thenReturn(5L);
            when(attemptHistoryRepository.count()).thenReturn(200L);
            when(attemptHistoryRepository.countByIsCorrect(true)).thenReturn(160L);
            when(attemptHistoryRepository.countByIsCorrect(false)).thenReturn(40L);
            when(attemptHistoryRepository.countDistinctLearners()).thenReturn(25L);
            when(userProgressRepository.countByLearningStatus(LearningStatus.LEARNED)).thenReturn(45L);
            when(attemptHistoryRepository.getAverageConfidence()).thenReturn(0.856);

            DashboardOverviewResponse res = analyticsService.getOverview();

            assertThat(res.getTotalVocabularies()).isEqualTo(50L);
            assertThat(res.getTotalCategories()).isEqualTo(5L);
            assertThat(res.getTotalAttempts()).isEqualTo(200L);
            assertThat(res.getCorrectAttempts()).isEqualTo(160L);
            assertThat(res.getIncorrectAttempts()).isEqualTo(40L);
            assertThat(res.getTotalLearners()).isEqualTo(25L);
            assertThat(res.getTotalLearnedWords()).isEqualTo(45L);
            assertThat(res.getOverallAccuracyRate()).isEqualTo(80.0);
            assertThat(res.getAverageConfidence()).isEqualTo(0.86);
        }

        @Test
        @DisplayName("Edge case: He thong chua co luot attempt nao (khong loi chia cho 0)")
        void getOverview_ZeroAttempts() {
            when(vocabularyRepository.count()).thenReturn(10L);
            when(categoryRepository.count()).thenReturn(2L);
            when(attemptHistoryRepository.count()).thenReturn(0L);
            when(attemptHistoryRepository.countByIsCorrect(true)).thenReturn(0L);
            when(attemptHistoryRepository.countByIsCorrect(false)).thenReturn(0L);
            when(attemptHistoryRepository.countDistinctLearners()).thenReturn(0L);
            when(userProgressRepository.countByLearningStatus(LearningStatus.LEARNED)).thenReturn(0L);
            when(attemptHistoryRepository.getAverageConfidence()).thenReturn(null);

            DashboardOverviewResponse res = analyticsService.getOverview();

            assertThat(res.getTotalAttempts()).isZero();
            assertThat(res.getOverallAccuracyRate()).isEqualTo(0.0);
            assertThat(res.getAverageConfidence()).isEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("getActivityTrends() tests")
    class GetActivityTrendsTests {

        @Test
        @DisplayName("Happy path: Tra ve dung so ngay va tinh dung so lieu theo tung ngay")
        void getActivityTrends_Success() {
            LocalDate today = LocalDate.now();
            AttemptTrendProjection a1 = mockAttempt(today.atTime(10, 0), true, 1L);
            AttemptTrendProjection a2 = mockAttempt(today.atTime(11, 0), false, 2L);
            AttemptTrendProjection a3 = mockAttempt(today.minusDays(1).atTime(15, 30), true, 1L);

            when(attemptHistoryRepository.findAttemptsBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                    .thenReturn(List.of(a3, a1, a2));

            List<DailyActivityResponse> trends = analyticsService.getActivityTrends(3);

            assertThat(trends).hasSize(3);

            // Hom nay (phan tu cuoi)
            DailyActivityResponse todayStat = trends.get(2);
            assertThat(todayStat.getDate()).isEqualTo(today);
            assertThat(todayStat.getTotalAttempts()).isEqualTo(2L);
            assertThat(todayStat.getCorrectAttempts()).isEqualTo(1L);
            assertThat(todayStat.getIncorrectAttempts()).isEqualTo(1L);
            assertThat(todayStat.getActiveLearners()).isEqualTo(2L);
            assertThat(todayStat.getAccuracyRate()).isEqualTo(50.0);

            // Hom qua
            DailyActivityResponse yesterdayStat = trends.get(1);
            assertThat(yesterdayStat.getDate()).isEqualTo(today.minusDays(1));
            assertThat(yesterdayStat.getTotalAttempts()).isEqualTo(1L);
            assertThat(yesterdayStat.getCorrectAttempts()).isEqualTo(1L);
            assertThat(yesterdayStat.getActiveLearners()).isEqualTo(1L);
            assertThat(yesterdayStat.getAccuracyRate()).isEqualTo(100.0);

            // 2 ngay truoc (khong co attempt)
            DailyActivityResponse twoDaysAgo = trends.get(0);
            assertThat(twoDaysAgo.getDate()).isEqualTo(today.minusDays(2));
            assertThat(twoDaysAgo.getTotalAttempts()).isZero();
            assertThat(twoDaysAgo.getAccuracyRate()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Edge case: Days <= 0 hoac days > 90 tu dong duoc gioi han an toan")
        void getActivityTrends_ClampedDays() {
            when(attemptHistoryRepository.findAttemptsBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                    .thenReturn(Collections.emptyList());

            List<DailyActivityResponse> minTrends = analyticsService.getActivityTrends(-5);
            assertThat(minTrends).hasSize(1); // toi thieu 1 ngay

            List<DailyActivityResponse> maxTrends = analyticsService.getActivityTrends(200);
            assertThat(maxTrends).hasSize(90); // toi da 90 ngay
        }

        private AttemptTrendProjection mockAttempt(LocalDateTime time, Boolean isCorrect, Long userId) {
            AttemptTrendProjection p = mock(AttemptTrendProjection.class);
            when(p.getAttemptedAt()).thenReturn(time);
            when(p.getIsCorrect()).thenReturn(isCorrect);
            when(p.getUserId()).thenReturn(userId);
            return p;
        }
    }

    @Nested
    @DisplayName("getVocabularyInsights() tests")
    class GetVocabularyInsightsTests {

        @Test
        @DisplayName("Happy path: Tra ve dung danh sach top, difficult, least practiced")
        void getVocabularyInsights_Success() {
            VocabularyPracticeProjection top1 = mockVocabProj(1L, "Xin chào", "Chào hỏi", 100L, 90L);
            VocabularyPracticeProjection diff1 = mockVocabProj(2L, "Cảm ơn", "Chào hỏi", 50L, 10L);
            VocabularyPracticeProjection least1 = mockVocabProj(3L, "Tạm biệt", "Chào hỏi", 2L, 1L);

            when(attemptHistoryRepository.findTopPracticedVocabularies(any(Pageable.class)))
                    .thenReturn(List.of(top1));
            when(attemptHistoryRepository.findMostDifficultVocabularies(eq(1L), any(Pageable.class)))
                    .thenReturn(List.of(diff1));
            when(vocabularyRepository.findLeastPracticedVocabularies(any(Pageable.class)))
                    .thenReturn(List.of(least1));

            VocabularyInsightResponse res = analyticsService.getVocabularyInsights(5);

            assertThat(res.getTopPracticed()).hasSize(1);
            assertThat(res.getTopPracticed().get(0).getWord()).isEqualTo("Xin chào");
            assertThat(res.getTopPracticed().get(0).getAccuracyRate()).isEqualTo(90.0);

            assertThat(res.getMostDifficult()).hasSize(1);
            assertThat(res.getMostDifficult().get(0).getWord()).isEqualTo("Cảm ơn");
            assertThat(res.getMostDifficult().get(0).getAccuracyRate()).isEqualTo(20.0);
            assertThat(res.getMostDifficult().get(0).getIncorrectCount()).isEqualTo(40L);

            assertThat(res.getLeastPracticed()).hasSize(1);
            assertThat(res.getLeastPracticed().get(0).getWord()).isEqualTo("Tạm biệt");
        }

        private VocabularyPracticeProjection mockVocabProj(Long id, String word, String catName, Long attempts, Long correct) {
            VocabularyPracticeProjection p = mock(VocabularyPracticeProjection.class);
            when(p.getVocabularyId()).thenReturn(id);
            when(p.getWord()).thenReturn(word);
            when(p.getCategoryName()).thenReturn(catName);
            when(p.getAttemptCount()).thenReturn(attempts);
            when(p.getCorrectCount()).thenReturn(correct);
            return p;
        }
    }

    @Nested
    @DisplayName("getCategoryAnalytics() tests")
    class GetCategoryAnalyticsTests {

        @Test
        @DisplayName("Happy path: Ghep dung so lieu practice va so luong tu learned theo tung category")
        void getCategoryAnalytics_Success() {
            CategoryPracticeProjection c1 = mock(CategoryPracticeProjection.class);
            when(c1.getCategoryId()).thenReturn(10L);
            when(c1.getCategoryName()).thenReturn("Gia đình");
            when(c1.getTotalVocabularies()).thenReturn(20L);
            when(c1.getTotalAttempts()).thenReturn(80L);
            when(c1.getCorrectAttempts()).thenReturn(60L);

            CategoryLearnedCountProjection l1 = mock(CategoryLearnedCountProjection.class);
            when(l1.getCategoryId()).thenReturn(10L);
            when(l1.getLearnedCount()).thenReturn(15L);

            when(categoryRepository.getCategoryPracticeStats()).thenReturn(List.of(c1));
            when(userProgressRepository.countLearnedPerCategory()).thenReturn(List.of(l1));

            List<CategoryAnalyticsResponse> res = analyticsService.getCategoryAnalytics();

            assertThat(res).hasSize(1);
            CategoryAnalyticsResponse item = res.get(0);
            assertThat(item.getCategoryId()).isEqualTo(10L);
            assertThat(item.getCategoryName()).isEqualTo("Gia đình");
            assertThat(item.getTotalVocabularies()).isEqualTo(20L);
            assertThat(item.getTotalAttempts()).isEqualTo(80L);
            assertThat(item.getLearnedCount()).isEqualTo(15L);
            assertThat(item.getAccuracyRate()).isEqualTo(75.0);
        }
    }
}
