package com.vsl.learning.controller;

import com.vsl.learning.dto.response.analytics.CategoryAnalyticsResponse;
import com.vsl.learning.dto.response.analytics.DailyActivityResponse;
import com.vsl.learning.dto.response.analytics.DashboardOverviewResponse;
import com.vsl.learning.dto.response.analytics.VocabularyInsightResponse;
import com.vsl.learning.dto.response.analytics.VocabularyStatItem;
import com.vsl.learning.service.PracticeAnalyticsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PracticeAnalyticsControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PracticeAnalyticsService practiceAnalyticsService;

    @InjectMocks
    private PracticeAnalyticsController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("GET /api/practice/analytics/overview -> 200 OK va cau truc ApiResponse chuan")
    void getOverview_Returns200() throws Exception {
        DashboardOverviewResponse data = DashboardOverviewResponse.builder()
                .totalVocabularies(100L)
                .totalCategories(10L)
                .totalAttempts(500L)
                .totalLearners(30L)
                .totalLearnedWords(80L)
                .overallAccuracyRate(75.5)
                .averageConfidence(0.88)
                .correctAttempts(380L)
                .incorrectAttempts(120L)
                .build();

        when(practiceAnalyticsService.getOverview()).thenReturn(data);

        mockMvc.perform(get("/api/practice/analytics/overview")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("Thành công"))
                .andExpect(jsonPath("$.data.totalVocabularies").value(100))
                .andExpect(jsonPath("$.data.totalAttempts").value(500))
                .andExpect(jsonPath("$.data.overallAccuracyRate").value(75.5));

        verify(practiceAnalyticsService).getOverview();
    }

    @Test
    @DisplayName("GET /api/practice/analytics/trends -> 200 OK voi danh sach theo ngay")
    void getActivityTrends_Returns200() throws Exception {
        DailyActivityResponse day1 = DailyActivityResponse.builder()
                .date(LocalDate.of(2026, 10, 14))
                .totalAttempts(50L)
                .correctAttempts(40L)
                .incorrectAttempts(10L)
                .activeLearners(15L)
                .accuracyRate(80.0)
                .build();

        when(practiceAnalyticsService.getActivityTrends(anyInt())).thenReturn(List.of(day1));

        mockMvc.perform(get("/api/practice/analytics/trends")
                        .param("days", "7")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data[0].totalAttempts").value(50))
                .andExpect(jsonPath("$.data[0].accuracyRate").value(80.0));

        verify(practiceAnalyticsService).getActivityTrends(7);
    }

    @Test
    @DisplayName("GET /api/practice/analytics/vocabulary-insights -> 200 OK voi insight top tu vung")
    void getVocabularyInsights_Returns200() throws Exception {
        VocabularyStatItem item = VocabularyStatItem.builder()
                .vocabularyId(1L)
                .word("Xin chào")
                .categoryName("Giao tiếp")
                .attemptCount(30L)
                .correctCount(25L)
                .incorrectCount(5L)
                .accuracyRate(83.3)
                .build();

        VocabularyInsightResponse response = VocabularyInsightResponse.builder()
                .topPracticed(List.of(item))
                .mostDifficult(List.of(item))
                .leastPracticed(List.of(item))
                .build();

        when(practiceAnalyticsService.getVocabularyInsights(anyInt())).thenReturn(response);

        mockMvc.perform(get("/api/practice/analytics/vocabulary-insights")
                        .param("limit", "5")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.topPracticed[0].word").value("Xin chào"))
                .andExpect(jsonPath("$.data.topPracticed[0].accuracyRate").value(83.3));

        verify(practiceAnalyticsService).getVocabularyInsights(5);
    }

    @Test
    @DisplayName("GET /api/practice/analytics/categories -> 200 OK voi thong ke tung category")
    void getCategoryAnalytics_Returns200() throws Exception {
        CategoryAnalyticsResponse cat1 = CategoryAnalyticsResponse.builder()
                .categoryId(1L)
                .categoryName("Chào hỏi")
                .totalVocabularies(15L)
                .totalAttempts(120L)
                .learnedCount(40L)
                .accuracyRate(85.0)
                .build();

        when(practiceAnalyticsService.getCategoryAnalytics()).thenReturn(List.of(cat1));

        mockMvc.perform(get("/api/practice/analytics/categories")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data[0].categoryName").value("Chào hỏi"))
                .andExpect(jsonPath("$.data[0].learnedCount").value(40));

        verify(practiceAnalyticsService).getCategoryAnalytics();
    }
}
