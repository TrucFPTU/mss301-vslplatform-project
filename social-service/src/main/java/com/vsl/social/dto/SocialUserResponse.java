package com.vsl.social.dto;

/** Người dùng kèm số liệu theo dõi + quan hệ với người xem (danh sách follow, kết quả tìm kiếm). */
public record SocialUserResponse(
        Long userId,
        String username,
        String fullName,
        String avatarUrl,
        String displayName,
        long followerCount,
        long followingCount,
        long friendCount,
        boolean followedByMe,
        boolean followsMe,
        boolean friend
) {
}
