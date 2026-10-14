package com.vsl.social.dto;

/** Trạng thái like sau khi like / bỏ like. */
public record LikeResponse(
        Long blogId,
        boolean liked,
        long likeCount
) {
}
