package com.vsl.social.dto;

import com.vsl.social.entity.UserFollow;

import java.time.Instant;

public record FollowResponse(
        Long followerId,
        Long followeeId,
        Instant createdAt
) {
    public static FollowResponse from(UserFollow f) {
        return new FollowResponse(f.getFollowerId(), f.getFolloweeId(), f.getCreatedAt());
    }
}
