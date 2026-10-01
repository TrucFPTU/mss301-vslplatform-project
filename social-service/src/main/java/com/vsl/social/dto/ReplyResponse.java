package com.vsl.social.dto;

import com.vsl.social.entity.CommentReply;

import java.time.Instant;

public record ReplyResponse(
        Long id,
        Long commentId,
        Long userId,
        String content,
        Instant createdAt,
        Instant updatedAt
) {
    public static ReplyResponse from(CommentReply r) {
        return new ReplyResponse(r.getId(), r.getComment().getId(), r.getUserId(), r.getContent(),
                r.getCreatedAt(), r.getUpdatedAt());
    }
}
