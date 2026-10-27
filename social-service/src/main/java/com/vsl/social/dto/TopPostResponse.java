package com.vsl.social.dto;

import com.vsl.social.entity.Blog;
import com.vsl.social.entity.BlogStatus;
import lombok.Builder;

import java.time.Instant;

@Builder
public record TopPostResponse(
        Long id,
        Long authorId,
        String title,
        BlogStatus status,
        long likeCount,
        long commentCount,
        long shareCount,
        long totalInteractions,
        Instant createdAt
) {
    public static TopPostResponse from(Blog b) {
        return TopPostResponse.builder()
                .id(b.getId())
                .authorId(b.getAuthorId())
                .title(b.getTitle())
                .status(b.getStatus())
                .likeCount(b.getLikeCount())
                .commentCount(b.getCommentCount())
                .shareCount(b.getShareCount())
                .totalInteractions(b.getLikeCount() + b.getCommentCount() + b.getShareCount())
                .createdAt(b.getCreatedAt())
                .build();
    }
}
