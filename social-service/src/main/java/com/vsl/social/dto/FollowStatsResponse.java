package com.vsl.social.dto;

/** Số người theo dõi / đang theo dõi của userId, và người xem hiện tại có đang theo dõi không. */
public record FollowStatsResponse(
        Long userId,
        long followers,
        long following,
        boolean followedByMe
) {
}
