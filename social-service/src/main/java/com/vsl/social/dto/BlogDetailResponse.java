package com.vsl.social.dto;

import com.vsl.social.entity.Blog;
import com.vsl.social.entity.BlogStatus;

import java.time.Instant;

/** Chi tiết bài viết: đủ nội dung + likedByMe (false nếu là khách). */
public record BlogDetailResponse(
        Long id,
        Long authorId,
        String title,
        String content,
        String thumbnailUrl,
        BlogStatus status,
        long likeCount,
        long commentCount,
        long shareCount,
        boolean likedByMe,
        Instant createdAt,
        Instant updatedAt
) {
    public static BlogDetailResponse from(Blog b, boolean likedByMe) {
        return new BlogDetailResponse(b.getId(), b.getAuthorId(), b.getTitle(), b.getContent(), b.getThumbnailUrl(),
                b.getStatus(), b.getLikeCount(), b.getCommentCount(), b.getShareCount(), likedByMe,
                b.getCreatedAt(), b.getUpdatedAt());
    }
}
