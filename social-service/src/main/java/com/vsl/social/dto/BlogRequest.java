package com.vsl.social.dto;

import com.vsl.social.entity.BlogStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Dữ liệu tạo / sửa bài viết.
 * status: DRAFT hoặc PUBLISHED (tạo mới bỏ trống = PUBLISHED; sửa bỏ trống = giữ nguyên).
 * Ảnh bìa đi qua POST /api/blogs/{id}/thumbnail (upload) - không nhận URL tự do trong request nữa
 * (bản trước ghi đè thumbnailUrl mỗi lần sửa, client không gửi lại là mất ảnh bìa).
 */
public record BlogRequest(
        @NotBlank @Size(max = 255) String title,
        @NotBlank @Size(max = 50_000) String content,
        BlogStatus status
) {
}
