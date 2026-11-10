package com.vsl.social.dto;

/**
 * Quan hệ theo dõi giữa người xem và targetUserId + số liệu của target.
 * friend = hai người theo dõi nhau (điều kiện để chia sẻ bài lên trang cá nhân của nhau).
 * blockedByMe = người xem đã chặn target; blocked = một trong hai người đã chặn người kia (không theo dõi /
 * nhắn tin / xem bài của nhau được).
 */
public record FollowStatusResponse(
        Long targetUserId,
        boolean followedByMe,
        boolean followsMe,
        boolean friend,
        long followerCount,
        long followingCount,
        long friendCount,
        boolean blockedByMe,
        boolean blocked
) {
}
