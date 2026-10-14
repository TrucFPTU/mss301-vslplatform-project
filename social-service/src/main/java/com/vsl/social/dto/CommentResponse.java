package com.vsl.social.dto;

import com.vsl.social.entity.BlogComment;

import java.time.Instant;

public record CommentResponse(
        Long id,
        Long blogId,
        Long userId,
        String content,
        Instant createdAt,
        Instant updatedAt
) {
    public static CommentResponse from(BlogComment c) {
        return new CommentResponse(c.getId(), c.getBlog().getId(), c.getUserId(), c.getContent(),
                c.getCreatedAt(), c.getUpdatedAt());
    }
}
