package com.vsl.identity.service;

/** Quản lý OTP dùng cho xác thực đăng ký và đặt lại mật khẩu. OTP lưu tạm trong bộ nhớ (5 phút). */
public interface OtpService {

    /** Sinh OTP 6 số cho email và lưu lại (ghi đè OTP cũ nếu có). */
    String generateAndStoreOtp(String email);

    /** Kiểm tra OTP có khớp và còn hạn không. */
    boolean verifyOtp(String email, String otp);

    /** Xóa OTP của email (sau khi dùng xong). */
    void clearOtp(String email);
}
