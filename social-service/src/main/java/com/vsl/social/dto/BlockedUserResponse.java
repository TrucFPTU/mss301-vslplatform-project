package com.vsl.social.dto;

import java.time.Instant;

/** Một người trong danh sách "đã chặn" của tôi. */
public record BlockedUserResponse(
        UserBriefResponse user,
        Instant blockedAt
) {
}
