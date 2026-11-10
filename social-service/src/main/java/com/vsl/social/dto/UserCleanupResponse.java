package com.vsl.social.dto;

/** Kết quả dọn dữ liệu của một người dùng đã xóa tài khoản (saga xóa tài khoản). */
public record UserCleanupResponse(int followsRemoved, int blocksRemoved, int notificationsRemoved, int blogsHidden) {
}
