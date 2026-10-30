package com.vsl.learning.controller;

import com.vsl.common.response.ApiResponse;
import com.vsl.common.response.PageResponse;
import com.vsl.learning.dto.response.AttemptResponse;
import com.vsl.learning.service.AttemptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Attempt", description = "Lịch sử luyện tập của người dùng hiện tại")
@RestController
@RequestMapping("/api/attempts")
@RequiredArgsConstructor
public class AttemptController {

    private final AttemptService attemptService;

    @Operation(summary = "Lịch sử luyện tập (phân trang, mới nhất trước)")
    @GetMapping
    public ApiResponse<PageResponse<AttemptResponse>> getMyAttempts(
            @AuthenticationPrincipal Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(attemptService.getMyAttempts(userId, page, size));
    }

    @Operation(summary = "Các lần luyện tập gần đây")
    @GetMapping("/recent")
    public ApiResponse<List<AttemptResponse>> getRecentAttempts(
            @AuthenticationPrincipal Long userId,
            @RequestParam(defaultValue = "5") int limit) {
        return ApiResponse.ok(attemptService.getRecentAttempts(userId, limit));
    }
}
