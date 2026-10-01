package com.vsl.learning.controller;

import com.vsl.common.response.ApiResponse;
import com.vsl.learning.dto.ai.EvaluationResponse;
import com.vsl.learning.dto.response.AchievementResponse;
import com.vsl.learning.dto.response.PracticeStatsResponse;
import com.vsl.learning.dto.response.UserProgressResponse;
import com.vsl.learning.service.AchievementService;
import com.vsl.learning.service.PracticeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "Practice", description = "Luyện tập, tiến trình, thống kê và thành tựu")
@RestController
@RequestMapping("/api/practice")
@RequiredArgsConstructor
public class PracticeController {

    private final PracticeService practiceService;
    private final AchievementService achievementService;

    @Operation(summary = "Nộp video luyện tập để AI chấm điểm",
            description = "learning-service gọi ai-service chấm điểm rồi lưu lịch sử, tiến trình, thành tựu.")
    @PostMapping(value = "/evaluate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<EvaluationResponse> evaluate(
            @AuthenticationPrincipal Long userId,
            @RequestPart("video") MultipartFile video,
            @RequestParam("expectedId") int expectedId,
            @RequestParam(value = "startFrac", defaultValue = "0.0") float startFrac,
            @RequestParam(value = "endFrac", defaultValue = "1.0") float endFrac) {
        return ApiResponse.ok(practiceService.evaluate(userId, video, expectedId, startFrac, endFrac),
                "Đánh giá hoàn tất");
    }

    @Operation(summary = "Tiến trình học của tôi")
    @GetMapping("/progress")
    public ApiResponse<List<UserProgressResponse>> getMyProgress(@AuthenticationPrincipal Long userId) {
        return ApiResponse.ok(practiceService.getMyProgress(userId));
    }

    @Operation(summary = "Thống kê luyện tập của tôi")
    @GetMapping("/stats")
    public ApiResponse<PracticeStatsResponse> getStats(@AuthenticationPrincipal Long userId) {
        return ApiResponse.ok(practiceService.getStats(userId));
    }

    @Operation(summary = "Thành tựu của tôi")
    @GetMapping("/achievements")
    public ApiResponse<List<AchievementResponse>> getAchievements(@AuthenticationPrincipal Long userId) {
        return ApiResponse.ok(achievementService.getAll(userId));
    }
}
