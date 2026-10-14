package com.vsl.social.dto;

import com.vsl.social.entity.Blog;
import com.vsl.social.entity.BlogStatus;

import java.time.Instant;

/** Bài viết dạng tóm tắt cho danh sách (chỉ trả đoạn trích nội dung). */
public record BlogResponse(
        Long id,
        Long authorId,
        String title,
        String excerpt,
        String thumbnailUrl,
        BlogStatus status,
        long likeCount,
        long commentCount,
        long shareCount,
        Instant createdAt,
        Instant updatedAt
) {
    private static final int EXCERPT_LENGTH = 200;

    public static BlogResponse from(Blog b) {
        String content = b.getContent();
        String excerpt = content.length() <= EXCERPT_LENGTH ? content : content.substring(0, EXCERPT_LENGTH) + "...";
        return new BlogResponse(b.getId(), b.getAuthorId(), b.getTitle(), excerpt, b.getThumbnailUrl(),
                b.getStatus(), b.getLikeCount(), b.getCommentCount(), b.getShareCount(),
                b.getCreatedAt(), b.getUpdatedAt());
    }
}
