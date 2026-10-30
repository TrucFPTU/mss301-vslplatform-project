package com.vsl.social.dto;

import com.vsl.social.entity.BlogStatus;

import java.time.Instant;

/** Chi tiết bài viết: đủ nội dung + trạng thái tương tác của người xem (false nếu là khách). */
public record BlogDetailResponse(
        Long id,
        UserBriefResponse author,
        String title,
        String content,
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
}
