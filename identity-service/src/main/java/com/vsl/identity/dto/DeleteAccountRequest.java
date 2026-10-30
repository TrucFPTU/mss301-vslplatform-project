package com.vsl.identity.dto;

import jakarta.validation.constraints.NotBlank;

/** Xác nhận mật khẩu khi tự xóa tài khoản (lộ access token không đủ để xóa tài khoản người khác). */
public record DeleteAccountRequest(@NotBlank(message = "Vui lòng nhập mật khẩu để xác nhận") String password) {
}
