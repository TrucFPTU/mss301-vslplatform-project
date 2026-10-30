package com.vsl.learning.controller;

import com.vsl.common.response.ApiResponse;
import com.vsl.learning.dto.request.AchievementRequest;
import com.vsl.learning.dto.response.AdminAchievementResponse;
import com.vsl.learning.service.AchievementAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Admin Achievement Management", description = "API Quản trị Thành tựu Học tập (Dành cho ADMIN)")
@RestController
@RequestMapping({"/api/practice/admin/achievements", "/api/achievements/admin"})
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class PracticeAdminAchievementController {

    private final AchievementAdminService achievementAdminService;

    @Operation(summary = "Danh sách toàn bộ thành tựu",
            description = "Lấy danh sách tất cả thành tựu trong hệ thống kèm số lượng học viên đã mở khoá.")
    @GetMapping
    public ApiResponse<List<AdminAchievementResponse>> getAllAchievements() {
        return ApiResponse.ok(achievementAdminService.getAllAchievements());
    }

    @Operation(summary = "Chi tiết thành tựu",
            description = "Xem chi tiết một thành tựu theo ID.")
    @GetMapping("/{id}")
    public ApiResponse<AdminAchievementResponse> getAchievementById(
            @Parameter(description = "ID của thành tựu")
            @PathVariable Long id) {
        return ApiResponse.ok(achievementAdminService.getAchievementById(id));
    }

    @Operation(summary = "Tạo mới thành tựu",
            description = "Admin tạo một danh mục thành tựu mới trong hệ thống.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AdminAchievementResponse> createAchievement(
            @Valid @RequestBody AchievementRequest request) {
        return ApiResponse.ok(achievementAdminService.createAchievement(request), "Tạo thành tựu thành công");
    }

    @Operation(summary = "Cập nhật thành tựu",
            description = "Admin cập nhật thông tin thành tựu (tên, mô tả, icon).")
    @PutMapping("/{id}")
    public ApiResponse<AdminAchievementResponse> updateAchievement(
            @Parameter(description = "ID của thành tựu")
            @PathVariable Long id,
            @Valid @RequestBody AchievementRequest request) {
        return ApiResponse.ok(achievementAdminService.updateAchievement(id, request), "Cập nhật thành tựu thành công");
    }

    @Operation(summary = "Xoá thành tựu",
            description = "Admin xoá thành tựu khỏi hệ thống cùng toàn bộ liên kết mở khoá của người dùng.")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteAchievement(
            @Parameter(description = "ID của thành tựu")
            @PathVariable Long id) {
        achievementAdminService.deleteAchievement(id);
        return ApiResponse.ok("Xoá thành tựu thành công");
    }

    @Operation(summary = "Trao tặng thành tựu cho học viên",
            description = "Admin mở khoá hoặc trao tặng thủ công thành tựu cho một người học.")
    @PostMapping("/{id}/grant")
    public ApiResponse<Void> grantAchievement(
            @Parameter(description = "ID của thành tựu")
            @PathVariable Long id,
            @Parameter(description = "ID của người học được trao tặng")
            @RequestParam Long userId) {
        achievementAdminService.grantAchievementToUser(id, userId);
        return ApiResponse.ok("Trao tặng thành tựu cho học viên thành công");
    }
}
