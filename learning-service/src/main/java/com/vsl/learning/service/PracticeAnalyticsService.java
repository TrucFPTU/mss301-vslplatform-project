package com.vsl.learning.service;

import com.vsl.learning.dto.response.analytics.CategoryAnalyticsResponse;
import com.vsl.learning.dto.response.analytics.DailyActivityResponse;
import com.vsl.learning.dto.response.analytics.DashboardOverviewResponse;
import com.vsl.learning.dto.response.analytics.VocabularyInsightResponse;

import java.util.List;

public interface PracticeAnalyticsService {

    /** Lay so lieu tong quan toan bo he thong hoc tap. */
    DashboardOverviewResponse getOverview();

    /** Lay xu huong luyen tap theo ngay trong N ngay gan nhat. */
    List<DailyActivityResponse> getActivityTrends(int days);

    /** Phan tich tu vung: top luyen tap, tu kho nhat, tu it duoc hoc nhat. */
    VocabularyInsightResponse getVocabularyInsights(int limit);

    /** Thong ke chi tiet tien do va do chinh xac theo tung danh muc. */
    List<CategoryAnalyticsResponse> getCategoryAnalytics();
}
