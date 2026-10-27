package com.vsl.social.dto;

import com.vsl.social.entity.BlogStatus;

import java.time.Instant;

/**
 * Bài viết dạng tóm tắt cho danh sách (chỉ trả đoạn trích nội dung).
 * deletionReason chỉ có với tác giả / admin khi bài bị gỡ (HIDDEN).
 */
public record BlogResponse(
        Long id,
        UserBriefResponse author,
        String title,
        String excerpt,
        String thumbnailUrl,
        BlogStatus status,
        String deletionReason,
        long likeCount,
        long commentCount,
        long shareCount,
        boolean likedByMe,
        boolean followedAuthor,
        boolean friendWithAuthor,
        Instant createdAt,
        Instant updatedAt
) {
    public static final int EXCERPT_LENGTH = 200;

    public static String excerpt(String content) {
        return content.length() <= EXCERPT_LENGTH ? content : content.substring(0, EXCERPT_LENGTH) + "...";
    }
}
