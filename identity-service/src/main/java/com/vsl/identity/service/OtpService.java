package com.vsl.identity.service;

/** Quản lý OTP dùng cho xác thực đăng ký và đặt lại mật khẩu. OTP lưu tạm trong bộ nhớ (5 phút). */
public interface OtpService {

    /**
     * Sinh OTP 6 số cho email và lưu lại (ghi đè OTP cũ nếu có).
     * Ném OTP_RESEND_TOO_SOON nếu mã trước được cấp chưa đủ thời gian chờ (chống spam email).
     */
    String generateAndStoreOtp(String email);

    /**
     * Kiểm tra OTP có khớp và còn hạn không. Mỗi mã chỉ cho nhập sai tối đa vài lần (chống dò mã);
     * quá số lần thì mã bị hủy và ném OTP_ATTEMPTS_EXCEEDED.
     */
    boolean verifyOtp(String email, String otp);

    /** Xóa OTP của email (sau khi dùng xong). */
    void clearOtp(String email);
}
