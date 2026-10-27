package com.vsl.social.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Admin gỡ bài: bắt buộc nêu lý do (hiển thị cho tác giả). */
public record HideBlogRequest(@NotBlank @Size(max = 1000) String reason) {
}
