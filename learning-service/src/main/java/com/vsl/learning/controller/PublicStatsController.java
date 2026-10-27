package com.vsl.learning.controller;

import com.vsl.common.response.ApiResponse;
import com.vsl.learning.dto.response.PublicStatsResponse;
import com.vsl.learning.service.PublicStatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Public Stats", description = "Số liệu công khai cho trang chủ")
@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicStatsController {

    private final PublicStatsService publicStatsService;

    @Operation(summary = "Số liệu trang chủ: tổng người dùng, từ vựng, tỉ lệ hài lòng")
    @GetMapping("/stats")
    public ApiResponse<PublicStatsResponse> getLandingStats() {
        return ApiResponse.ok(publicStatsService.getLandingStats());
    }
}
