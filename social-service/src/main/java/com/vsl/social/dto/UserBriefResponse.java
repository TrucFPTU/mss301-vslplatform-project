package com.vsl.social.dto;

import com.vsl.social.dto.remote.RemoteDtos.UserSummary;

/**
 * Người dùng gắn với một nội dung (tác giả, người bình luận, người like...). Tên / avatar lấy từ
 * identity-service; identity tạm lỗi thì chỉ còn userId (các trường còn lại null).
 */
public record UserBriefResponse(
        Long userId,
        String username,
        String fullName,
        String avatarUrl,
        String displayName
) {
    public static UserBriefResponse of(Long userId, UserSummary s) {
        if (userId == null) {
            return null;
        }
        if (s == null) {
            return new UserBriefResponse(userId, null, null, null, null);
        }
        return new UserBriefResponse(userId, s.username(), s.fullName(), s.avatarUrl(), s.displayName());
    }
}
