package com.vsl.identity.dto;

import com.vsl.identity.entity.User;
import com.vsl.identity.entity.UserStatus;

/**
 * Hồ sơ công khai của một người dùng (KHÔNG có email / role / trạng thái khóa).
 * Dùng cho trang cá nhân và cho service khác (social) hiển thị tên + avatar.
 * deleted = tài khoản đã xóa (đã ẩn danh hóa): social dựa vào đây để không cho theo dõi / nhắn tin / chặn.
 */
public record UserSummaryResponse(
        Long userId,
        String username,
        String fullName,
        String avatarUrl,
        boolean deleted
) {
    public UserSummaryResponse(Long userId, String username, String fullName, String avatarUrl) {
        this(userId, username, fullName, avatarUrl, false);
    }

    public static UserSummaryResponse from(User u) {
        return new UserSummaryResponse(u.getUserId(), u.getUsername(), u.getFullName(), u.getAvatarUrl(),
                u.getStatus() == UserStatus.DELETED);
    }
}
