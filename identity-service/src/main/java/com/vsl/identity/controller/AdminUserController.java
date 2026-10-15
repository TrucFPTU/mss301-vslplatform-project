package com.vsl.identity.controller;

import com.vsl.common.response.ApiResponse;
import com.vsl.common.response.PageResponse;
import com.vsl.identity.dto.request.AdminCreateUserRequest;
import com.vsl.identity.dto.request.UserRoleUpdateRequest;
import com.vsl.identity.dto.request.UserStatusUpdateRequest;
import com.vsl.identity.dto.response.AdminUserResponse;
import com.vsl.identity.dto.response.UserStatisticsResponse;
import com.vsl.identity.entity.Role;
import com.vsl.identity.entity.UserStatus;
import com.vsl.identity.service.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Admin User Management", description = "API Quản trị Tài khoản Người dùng (Dành cho ADMIN)")
@RestController
@RequestMapping({"/api/users/admin", "/api/admin/users"})
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;

    @Operation(summary = "Danh sách người dùng hệ thống",
            description = "Lấy danh sách người dùng phân trang, hỗ trợ tìm kiếm theo từ khoá (username/email/fullName) và lọc theo role, status.")
    @GetMapping
    public ApiResponse<PageResponse<AdminUserResponse>> getUsers(
            @Parameter(description = "Từ khoá tìm kiếm theo username, email hoặc tên")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "Lọc theo vai trò (ROLE_USER, ROLE_ADMIN)")
            @RequestParam(required = false) Role role,
            @Parameter(description = "Lọc theo trạng thái (ACTIVE, BANNED, PENDING_VERIFICATION)")
            @RequestParam(required = false) UserStatus status,
            @Parameter(description = "Số trang (bắt đầu từ 0)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Số lượng bản ghi mỗi trang")
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(adminUserService.getUsers(keyword, role, status, page, size));
    }

    @Operation(summary = "Thống kê người dùng",
            description = "Thống kê số lượng người dùng hệ thống: tổng số, số tài khoản đang hoạt động, số tài khoản bị khoá, số lượng admin.")
    @GetMapping("/statistics")
    public ApiResponse<UserStatisticsResponse> getUserStatistics() {
        return ApiResponse.ok(adminUserService.getUserStatistics());
    }

    @Operation(summary = "Chi tiết người dùng",
            description = "Xem thông tin chi tiết của một tài khoản theo ID.")
    @GetMapping("/{id}")
    public ApiResponse<AdminUserResponse> getUserById(
            @Parameter(description = "ID người dùng")
            @PathVariable Long id) {
        return ApiResponse.ok(adminUserService.getUserById(id));
    }

    @Operation(summary = "Tạo mới tài khoản người dùng / quản trị viên",
            description = "Admin tạo tài khoản mới trực tiếp trong hệ thống.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AdminUserResponse> createUser(
            @Valid @RequestBody AdminCreateUserRequest request) {
        return ApiResponse.ok(adminUserService.createUser(request), "Tạo tài khoản người dùng thành công");
    }

    @Operation(summary = "Khoá hoặc mở khoá tài khoản",
            description = "Admin thay đổi trạng thái hoạt động của tài khoản (ACTIVE: hoạt động, BANNED: khoá tài khoản vi phạm).")
    @PutMapping("/{id}/status")
    public ApiResponse<AdminUserResponse> updateUserStatus(
            @Parameter(description = "ID người dùng")
            @PathVariable Long id,
            @Valid @RequestBody UserStatusUpdateRequest request) {
        String msg = request.getStatus() == UserStatus.BANNED ? "Đã khoá tài khoản người dùng" : "Đã mở khoá tài khoản người dùng";
        return ApiResponse.ok(adminUserService.updateUserStatus(id, request), msg);
    }

    @Operation(summary = "Phân quyền vai trò người dùng",
            description = "Admin nâng cấp hoặc hạ cấp vai trò tài khoản (ROLE_USER <-> ROLE_ADMIN).")
    @PutMapping("/{id}/role")
    public ApiResponse<AdminUserResponse> updateUserRole(
            @Parameter(description = "ID người dùng")
            @PathVariable Long id,
            @Valid @RequestBody UserRoleUpdateRequest request) {
        return ApiResponse.ok(adminUserService.updateUserRole(id, request), "Cập nhật quyền người dùng thành công");
    }
}
