package com.vsl.social.controller;

import com.vsl.common.response.ApiResponse;
import com.vsl.social.dto.UserCleanupResponse;
import com.vsl.social.service.UserCleanupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API nội bộ cho service khác gọi (không dành cho người dùng). Chỉ nhận token role SERVICE: token này chỉ
 * service có JWT_SECRET chung mới ký được. {@code /api/internal/**} KHÔNG có trong route của api-gateway,
 * nên chỉ gọi được bên trong mạng Docker qua Eureka.
 */
@Tag(name = "Internal", description = "API nội bộ giữa các service")
@RestController
@RequestMapping("/api/internal/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SERVICE')")
public class InternalUserController {

    private final UserCleanupService userCleanupService;

    @PostMapping("/{userId}/deleted")
    @Operation(summary = "Dọn dữ liệu của người dùng đã xóa tài khoản (identity gọi trong saga xóa tài khoản)")
    public ApiResponse<UserCleanupResponse> userDeleted(@PathVariable Long userId) {
        return ApiResponse.ok(userCleanupService.cleanupDeletedUser(userId));
    }
}
