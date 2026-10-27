package com.vsl.social.dto;

import java.time.Instant;

public record CommentResponse(
        Long id,
        Long blogId,
        UserBriefResponse user,
        UserBriefResponse mentionedUser,
        String content,
        long replyCount,
        long likeCount,
        boolean likedByMe,
        Instant createdAt,
        Instant updatedAt
) {
}
