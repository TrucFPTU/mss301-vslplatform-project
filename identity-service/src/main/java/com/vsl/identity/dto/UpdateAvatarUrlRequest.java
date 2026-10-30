package com.vsl.identity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Đặt avatar bằng đường dẫn ảnh có sẵn (bê từ monolith: PUT /api/users/avatar). */
public record UpdateAvatarUrlRequest(
        @NotBlank(message = "Vui lòng nhập đường dẫn ảnh")
        @Size(max = 1024, message = "Đường dẫn ảnh tối đa 1024 ký tự")
        @Pattern(regexp = "^https?://\\S+$", message = "Đường dẫn ảnh phải bắt đầu bằng http:// hoặc https://")
        String avatarUrl) {
}
