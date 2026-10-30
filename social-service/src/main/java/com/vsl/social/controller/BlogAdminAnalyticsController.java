package com.vsl.social.controller;

import com.vsl.common.response.ApiResponse;
import com.vsl.social.dto.SocialDashboardOverviewResponse;
import com.vsl.social.dto.TopCreatorResponse;
import com.vsl.social.dto.TopPostResponse;
import com.vsl.social.service.SocialAnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Admin Social Analytics", description = "API Thống kê & Dashboard Mạng xã hội (Dành cho ADMIN)")
@RestController
@RequestMapping({"/api/blogs/admin/analytics", "/api/admin/social/analytics"})
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class BlogAdminAnalyticsController {

    private final SocialAnalyticsService socialAnalyticsService;

    @Operation(summary = "Tổng quan mạng xã hội",
            description = "Lấy số liệu tổng quan về bài viết (published, hidden, draft), tổng lượt like/comment/share và số báo cáo vi phạm.")
    @GetMapping("/overview")
    public ApiResponse<SocialDashboardOverviewResponse> getOverview() {
        return ApiResponse.ok(socialAnalyticsService.getOverview());
    }

    @Operation(summary = "Top bài viết tương tác cao nhất",
            description = "Danh sách các bài viết có tổng lượt tương tác (like + comment + share) cao nhất hệ thống.")
    @GetMapping("/top-posts")
    public ApiResponse<List<TopPostResponse>> getTopEngagedPosts(
            @Parameter(description = "Số lượng bài viết cần lấy (mặc định 10)")
            @RequestParam(defaultValue = "10") int limit) {
        return ApiResponse.ok(socialAnalyticsService.getTopEngagedPosts(limit));
    }

    @Operation(summary = "Top người đóng góp bài viết",
            description = "Danh sách những người dùng đăng nhiều bài viết và nhận được nhiều lượt tương tác nhất.")
    @GetMapping("/top-creators")
    public ApiResponse<List<TopCreatorResponse>> getTopCreators(
            @Parameter(description = "Số lượng người sáng tạo cần lấy (mặc định 5)")
            @RequestParam(defaultValue = "5") int limit) {
        return ApiResponse.ok(socialAnalyticsService.getTopCreators(limit));
    }
}
