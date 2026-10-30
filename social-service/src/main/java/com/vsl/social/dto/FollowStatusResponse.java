package com.vsl.social.dto;

/**
 * Quan hệ theo dõi giữa người xem và targetUserId + số liệu của target.
 * friend = hai người theo dõi nhau (điều kiện để chia sẻ bài lên trang cá nhân của nhau).
 */
public record FollowStatusResponse(
        Long targetUserId,
        boolean followedByMe,
        boolean followsMe,
        boolean friend,
        long followerCount,
        long followingCount,
        long friendCount
) {
}
