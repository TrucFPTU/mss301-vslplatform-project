package com.vsl.social.dto;

import java.time.Instant;

public record ReplyResponse(
        Long id,
        Long commentId,
        UserBriefResponse user,
        UserBriefResponse mentionedUser,
        String content,
        long likeCount,
        boolean likedByMe,
        Instant createdAt,
        Instant updatedAt
) {
}
