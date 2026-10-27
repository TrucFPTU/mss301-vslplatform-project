package com.vsl.social.dto;

/** Trạng thái like của một bình luận/trả lời sau khi like hoặc bỏ like. */
public record CommentLikeResponse(
        Long targetId,
        boolean liked,
        long likeCount
) {
}
