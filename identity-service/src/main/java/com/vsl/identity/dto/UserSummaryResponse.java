package com.vsl.identity.dto;

import com.vsl.identity.entity.User;

/**
 * Hồ sơ công khai của một người dùng (KHÔNG có email / role / trạng thái).
 * Dùng cho trang cá nhân và cho service khác (social) hiển thị tên + avatar.
 */
public record UserSummaryResponse(
        Long userId,
        String username,
        String fullName,
        String avatarUrl
) {
    public static UserSummaryResponse from(User u) {
        return new UserSummaryResponse(u.getUserId(), u.getUsername(), u.getFullName(), u.getAvatarUrl());
    }
}
