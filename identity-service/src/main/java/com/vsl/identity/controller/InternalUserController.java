package com.vsl.identity.controller;

import com.vsl.common.response.ApiResponse;
import com.vsl.identity.dto.StreakReminderRequest;
import com.vsl.identity.dto.StreakReminderResult;
import com.vsl.identity.service.StreakReminderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API nội bộ cho service khác gọi (không dành cho người dùng).
 * Chỉ nhận token role SERVICE - token này chỉ service có JWT_SECRET chung mới ký được.
 */
@Tag(name = "Internal", description = "API nội bộ giữa các service")
@RestController
@RequestMapping("/api/users/internal")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SERVICE')")
public class InternalUserController {

    private final StreakReminderService streakReminderService;

    @PostMapping("/streak-reminders")
    @Operation(summary = "Gửi mail nhắc streak (learning-service gọi theo lịch)")
    public ApiResponse<StreakReminderResult> sendStreakReminders(@Valid @RequestBody StreakReminderRequest request) {
        return ApiResponse.ok(streakReminderService.sendReminders(request));
    }
}
