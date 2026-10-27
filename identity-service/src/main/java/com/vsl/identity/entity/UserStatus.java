package com.vsl.identity.entity;

public enum UserStatus {
    ACTIVE,
    BANNED,
    PENDING_VERIFICATION,
    /** Tài khoản đã xóa (ẩn danh hóa, không đăng nhập được, không khôi phục). Chỉ đặt qua API xóa. */
    DELETED
}
