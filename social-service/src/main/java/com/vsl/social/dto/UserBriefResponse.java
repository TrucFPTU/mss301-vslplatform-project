package com.vsl.social.dto;

import com.vsl.social.dto.remote.RemoteDtos.UserSummary;

/**
 * Người dùng gắn với một nội dung (tác giả, người bình luận, người like...). Tên / avatar lấy từ
 * identity-service; identity tạm lỗi thì chỉ còn userId (các trường còn lại null).
 * deleted = tài khoản đã xóa (FE hiện "tài khoản đã bị xóa" thay vì nút theo dõi / nhắn tin).
 */
public record UserBriefResponse(
        Long userId,
        String username,
        String fullName,
        String avatarUrl,
        String displayName,
        boolean deleted
) {
    public UserBriefResponse(Long userId, String username, String fullName, String avatarUrl, String displayName) {
        this(userId, username, fullName, avatarUrl, displayName, false);
    }

    public static UserBriefResponse of(Long userId, UserSummary s) {
        if (userId == null) {
            return null;
        }
        if (s == null) {
            return new UserBriefResponse(userId, null, null, null, null);
        }
        return new UserBriefResponse(userId, s.username(), s.fullName(), s.avatarUrl(), s.displayName(), s.deleted());
    }
}
