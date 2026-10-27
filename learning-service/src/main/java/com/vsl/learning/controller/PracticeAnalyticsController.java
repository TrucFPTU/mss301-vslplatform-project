package com.vsl.learning.controller;

import com.vsl.common.response.ApiResponse;
import com.vsl.learning.dto.response.analytics.CategoryAnalyticsResponse;
import com.vsl.learning.dto.response.analytics.DailyActivityResponse;
import com.vsl.learning.dto.response.analytics.DashboardOverviewResponse;
import com.vsl.learning.dto.response.analytics.VocabularyInsightResponse;
import com.vsl.learning.service.PracticeAnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Practice Analytics", description = "API Thống kê & Báo cáo Dashboard Học tập (Dành cho ADMIN)")
@RestController
@RequestMapping({"/api/practice/analytics", "/api/admin/analytics"})
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasRole('ADMIN')")
public class PracticeAnalyticsController {

    private final PracticeAnalyticsService practiceAnalyticsService;

    @Operation(summary = "Thống kê tổng quan hệ thống",
            description = "Trả về tổng số từ vựng, danh mục, lượt luyện tập, học viên, tỷ lệ đúng/sai và độ tin cậy AI trung bình.")
    @GetMapping("/overview")
    public ApiResponse<DashboardOverviewResponse> getOverview() {
        return ApiResponse.ok(practiceAnalyticsService.getOverview());
    }

    @Operation(summary = "Xu hướng luyện tập theo ngày",
            description = "Trả về chuỗi số liệu luyện tập theo từng ngày (mặc định 7 ngày, tối đa 90 ngày) phục vụ biểu đồ.")
    @GetMapping("/trends")
    public ApiResponse<List<DailyActivityResponse>> getActivityTrends(
            @Parameter(description = "Số ngày gần nhất cần thống kê (1 - 90)", example = "7")
            @RequestParam(value = "days", defaultValue = "7") @Min(1) @Max(90) int days) {
        return ApiResponse.ok(practiceAnalyticsService.getActivityTrends(days));
    }

    @Operation(summary = "Phân tích chuyên sâu về từ vựng",
            description = "Trả về top từ vựng được luyện tập nhiều nhất, từ khó nhất (tỷ lệ sai cao) và từ ít được luyện tập nhất.")
    @GetMapping("/vocabulary-insights")
    public ApiResponse<VocabularyInsightResponse> getVocabularyInsights(
            @Parameter(description = "Số lượng từ vựng mỗi nhóm cần lấy (1 - 50)", example = "5")
            @RequestParam(value = "limit", defaultValue = "5") @Min(1) @Max(50) int limit) {
        return ApiResponse.ok(practiceAnalyticsService.getVocabularyInsights(limit));
    }

    @Operation(summary = "Thống kê tiến độ theo danh mục",
            description = "Trả về số lượng từ, số lượt luyện tập, số từ đã hoàn thành và tỷ lệ chính xác theo từng danh mục.")
    @GetMapping("/categories")
    public ApiResponse<List<CategoryAnalyticsResponse>> getCategoryAnalytics() {
        return ApiResponse.ok(practiceAnalyticsService.getCategoryAnalytics());
    }
}
