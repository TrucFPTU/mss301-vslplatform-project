package com.vsl.identity.controller;

import com.vsl.common.response.ApiResponse;
import com.vsl.identity.dto.ForgotPasswordRequest;
import com.vsl.identity.dto.LoginRequest;
import com.vsl.identity.dto.RefreshTokenRequest;
import com.vsl.identity.dto.RegisterRequest;
import com.vsl.identity.dto.ResetPasswordRequest;
import com.vsl.identity.dto.SendRegisterOtpRequest;
import com.vsl.identity.dto.TokenResponse;
import com.vsl.identity.dto.VerifyOtpRequest;
import com.vsl.identity.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Các API xác thực, đăng ký, đăng nhập và refresh token")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/send-register-otp")
    @Operation(summary = "Gửi OTP đăng ký", description = "Gửi mã OTP tới email trước khi đăng ký")
    public ApiResponse<Void> sendRegisterOtp(@Valid @RequestBody SendRegisterOtpRequest request) {
        authService.sendRegisterOtp(request);
        return ApiResponse.ok("Đã gửi mã OTP tới email của bạn");
    }

    @PostMapping("/register")
    @Operation(summary = "Đăng ký tài khoản mới", description = "Xác thực OTP rồi tạo tài khoản, trả về Access Token + Refresh Token")
    public ApiResponse<TokenResponse> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest) {
        String ipAddress = extractIpAddress(httpRequest);
        String deviceInfo = httpRequest.getHeader("User-Agent");
        TokenResponse response = authService.register(request, ipAddress, deviceInfo);
        return ApiResponse.ok(response, "Đăng ký tài khoản thành công");
    }

    @PostMapping("/login")
    @Operation(summary = "Đăng nhập", description = "Đăng nhập bằng username hoặc email, trả về token")
    public ApiResponse<TokenResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        String ipAddress = extractIpAddress(httpRequest);
        String deviceInfo = httpRequest.getHeader("User-Agent");
        TokenResponse response = authService.login(request, ipAddress, deviceInfo);
        return ApiResponse.ok(response, "Đăng nhập thành công");
    }

    @PostMapping("/refresh")
    @Operation(summary = "Làm mới Access Token", description = "Nhận Refresh Token hợp lệ và trả về cặp Token mới")
    public ApiResponse<TokenResponse> refresh(
            @Valid @RequestBody RefreshTokenRequest request) {
        TokenResponse response = authService.refreshToken(request);
        return ApiResponse.ok(response, "Làm mới token thành công");
    }

    @PostMapping("/logout")
    @Operation(summary = "Đăng xuất", description = "Thu hồi Refresh Token")
    public ApiResponse<Void> logout(
            @RequestBody(required = false) RefreshTokenRequest request) {
        if (request != null && request.getRefreshToken() != null) {
            authService.logout(request.getRefreshToken());
        }
        return ApiResponse.ok("Đăng xuất thành công");
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Quên mật khẩu", description = "Gửi OTP đặt lại mật khẩu tới email đã đăng ký")
    public ApiResponse<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ApiResponse.ok("Đã gửi mã OTP đặt lại mật khẩu tới email của bạn");
    }

    @PostMapping("/verify-otp")
    @Operation(summary = "Xác thực OTP", description = "Kiểm tra OTP hợp lệ trước khi đặt lại mật khẩu")
    public ApiResponse<Void> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        authService.verifyOtp(request);
        return ApiResponse.ok("Mã OTP hợp lệ");
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Đặt lại mật khẩu", description = "Đặt lại mật khẩu mới sau khi xác thực OTP")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ApiResponse.ok("Đặt lại mật khẩu thành công");
    }

    private String extractIpAddress(HttpServletRequest request) {
        String clientIp = request.getHeader("X-Forwarded-For");
        if (clientIp == null || clientIp.isBlank() || "unknown".equalsIgnoreCase(clientIp)) {
            clientIp = request.getRemoteAddr();
        } else if (clientIp.contains(",")) {
            clientIp = clientIp.split(",")[0].trim();
        }
        return clientIp;
    }
}
