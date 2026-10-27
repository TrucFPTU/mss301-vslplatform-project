package com.vsl.identity.controller;

import com.vsl.common.response.ApiResponse;
import com.vsl.identity.dto.ChangePasswordRequest;
import com.vsl.identity.dto.PublicUserStatsResponse;
import com.vsl.identity.dto.UpdateProfileRequest;
import com.vsl.identity.dto.UserDto;
import com.vsl.identity.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "Các API quản lý thông tin người dùng và tài khoản")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @Operation(summary = "Lấy thông tin tài khoản hiện tại", description = "Trả về thông tin chi tiết của người dùng đang đăng nhập")
    public ApiResponse<UserDto> getMyProfile(@AuthenticationPrincipal Long userId) {
        return ApiResponse.ok(userService.getProfile(userId));
    }

    @PutMapping("/me")
    @Operation(summary = "Cập nhật hồ sơ cá nhân", description = "Cập nhật họ tên, tiểu sử, ảnh đại diện")
    public ApiResponse<UserDto> updateMyProfile(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ApiResponse.ok(userService.updateProfile(userId, request), "Cập nhật thông tin cá nhân thành công");
    }

    @PutMapping("/me/password")
    @Operation(summary = "Đổi mật khẩu", description = "Xác nhận mật khẩu cũ và đặt mật khẩu mới")
    public ApiResponse<Void> changePassword(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(userId, request);
        return ApiResponse.ok("Đổi mật khẩu thành công");
    }

    @GetMapping("/public-stats")
    @Operation(summary = "Số liệu người dùng công khai", description = "Tổng người dùng và học viên - learning-service gọi để tổng hợp thống kê")
    public ApiResponse<PublicUserStatsResponse> getPublicStats() {
        return ApiResponse.ok(userService.getPublicStats());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy thông tin người dùng theo ID", description = "Dùng cho hiển thị công khai hoặc các service khác gọi sang")
    public ApiResponse<UserDto> getUserById(@PathVariable("id") Long id) {
        return ApiResponse.ok(userService.getUserById(id));
    }
}
