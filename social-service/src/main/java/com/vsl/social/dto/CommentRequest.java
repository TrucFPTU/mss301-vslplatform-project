package com.vsl.social.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Nội dung bình luận hoặc trả lời bình luận. */
public record CommentRequest(
        @NotBlank @Size(max = 2000) String content
) {
}
