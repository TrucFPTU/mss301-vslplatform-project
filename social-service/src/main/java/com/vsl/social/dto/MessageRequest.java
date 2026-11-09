package com.vsl.social.dto;

import com.vsl.social.entity.DirectMessage;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MessageRequest(
        @NotBlank(message = "Tin nhắn không được để trống")
        @Size(max = DirectMessage.MAX_LENGTH, message = "Tin nhắn tối đa 1000 ký tự")
        String content
) {
}
