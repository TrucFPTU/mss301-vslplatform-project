package com.vsl.identity.service;

import com.vsl.identity.dto.ForgotPasswordRequest;
import com.vsl.identity.dto.LoginRequest;
import com.vsl.identity.dto.RefreshTokenRequest;
import com.vsl.identity.dto.RegisterRequest;
import com.vsl.identity.dto.ResetPasswordRequest;
import com.vsl.identity.dto.SendRegisterOtpRequest;
import com.vsl.identity.dto.TokenResponse;
import com.vsl.identity.dto.VerifyOtpRequest;

public interface AuthService {

    /** Gửi OTP tới email trước khi đăng ký (kiểm tra username/email chưa tồn tại). */
    void sendRegisterOtp(SendRegisterOtpRequest request);

    TokenResponse register(RegisterRequest request, String ipAddress, String deviceInfo);

    TokenResponse login(LoginRequest request, String ipAddress, String deviceInfo);

    TokenResponse refreshToken(RefreshTokenRequest request);

    void logout(String refreshToken);

    /** Gửi OTP đặt lại mật khẩu tới email đã đăng ký. */
    void forgotPassword(ForgotPasswordRequest request);

    /** Kiểm tra OTP hợp lệ (không đổi mật khẩu). */
    void verifyOtp(VerifyOtpRequest request);

    /** Đặt lại mật khẩu sau khi xác thực OTP. */
    void resetPassword(ResetPasswordRequest request);
}
