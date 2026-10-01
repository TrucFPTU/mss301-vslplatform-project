package com.vsl.social.dto;

import com.vsl.social.entity.BlogStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Dữ liệu tạo / sửa bài viết.
 * status: DRAFT hoặc PUBLISHED (bỏ trống = PUBLISHED). HIDDEN chỉ admin đặt.
 */
public record BlogRequest(
        @NotBlank @Size(max = 255) String title,
        @NotBlank @Size(max = 50_000) String content,
        @Size(max = 1000) String thumbnailUrl,
        BlogStatus status
) {
}
