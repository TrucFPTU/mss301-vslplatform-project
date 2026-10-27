package com.vsl.identity.controller;

import com.vsl.common.response.ApiResponse;
import com.vsl.common.response.PageResponse;
import com.vsl.identity.dto.ChangePasswordRequest;
import com.vsl.identity.dto.DeleteAccountRequest;
import com.vsl.identity.dto.PublicUserStatsResponse;
import com.vsl.identity.dto.UpdateProfileRequest;
import com.vsl.identity.dto.UserDto;
import com.vsl.identity.dto.UserSummaryResponse;
import com.vsl.identity.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

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

    @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Tải avatar", description = "JPEG/PNG/WEBP/GIF, tối đa 5MB; thay ảnh cũ")
    public ApiResponse<UserDto> uploadAvatar(@AuthenticationPrincipal Long userId,
                                             @RequestPart("image") MultipartFile image) {
        return ApiResponse.ok(userService.uploadAvatar(userId, image), "Đã cập nhật avatar");
    }

    @DeleteMapping("/me/avatar")
    @Operation(summary = "Xóa avatar")
    public ApiResponse<UserDto> removeAvatar(@AuthenticationPrincipal Long userId) {
        return ApiResponse.ok(userService.removeAvatar(userId), "Đã xóa avatar");
    }

    @DeleteMapping("/me")
    @Operation(summary = "Xóa tài khoản của tôi", description = "Xác nhận bằng mật khẩu; tài khoản bị ẩn danh hóa, đăng xuất mọi thiết bị")
    public ApiResponse<Void> deleteMyAccount(@AuthenticationPrincipal Long userId,
                                             @Valid @RequestBody DeleteAccountRequest request) {
        userService.deleteMyAccount(userId, request.password());
        return ApiResponse.ok("Đã xóa tài khoản");
    }

    @PatchMapping("/me/notifications")
    @Operation(summary = "Bật/tắt email nhắc nhở streak", description = "Mặc định tắt; bật để nhận mail nhắc khi sắp mất chuỗi ngày học")
    public ApiResponse<UserDto> updateNotificationSettings(
            @AuthenticationPrincipal Long userId,
            @RequestParam boolean emailNotificationsEnabled) {
        return ApiResponse.ok(userService.updateNotificationSettings(userId, emailNotificationsEnabled),
                "Đã cập nhật cài đặt thông báo");
    }

    @GetMapping("/public-stats")
    @Operation(summary = "Số liệu người dùng công khai", description = "Tổng người dùng và học viên - learning-service gọi để tổng hợp thống kê")
    public ApiResponse<PublicUserStatsResponse> getPublicStats() {
        return ApiResponse.ok(userService.getPublicStats());
    }

    @GetMapping("/summaries")
    @Operation(summary = "Tra cứu hàng loạt hồ sơ công khai", description = "ids=1,2,3 (tối đa 100). Social dùng để hiển thị tên + avatar")
    public ApiResponse<List<UserSummaryResponse>> getSummaries(@RequestParam List<Long> ids) {
        return ApiResponse.ok(userService.getSummaries(ids));
    }

    @GetMapping("/search")
    @Operation(summary = "Tìm người dùng theo họ tên / username")
    public ApiResponse<PageResponse<UserSummaryResponse>> searchUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(userService.searchUsers(keyword, page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Hồ sơ công khai theo ID", description = "Không trả email / role - chỉ tên, username, avatar")
    public ApiResponse<UserSummaryResponse> getPublicProfile(@PathVariable("id") Long id) {
        return ApiResponse.ok(userService.getPublicProfile(id));
    }
}
