package com.vsl.social.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Nội dung bình luận hoặc trả lời bình luận.
 * mentionedUserId: người được nhắc tên (tùy chọn) - nhận thông báo MENTION.
 */
public record CommentRequest(
        @NotBlank @Size(max = 2000) String content,
        Long mentionedUserId
) {
    public CommentRequest(String content) {
        this(content, null);
    }
}
