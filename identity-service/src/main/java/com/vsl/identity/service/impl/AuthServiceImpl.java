package com.vsl.identity.service.impl;

import com.vsl.common.exception.AppException;
import com.vsl.common.exception.ErrorCode;
import com.vsl.common.security.JwtProperties;
import com.vsl.common.security.JwtService;
import com.vsl.identity.dto.ForgotPasswordRequest;
import com.vsl.identity.dto.LoginRequest;
import com.vsl.identity.dto.RefreshTokenRequest;
import com.vsl.identity.dto.RegisterRequest;
import com.vsl.identity.dto.ResetPasswordRequest;
import com.vsl.identity.dto.RotationResult;
import com.vsl.identity.dto.SendRegisterOtpRequest;
import com.vsl.identity.dto.TokenResponse;
import com.vsl.identity.dto.VerifyOtpRequest;
import com.vsl.identity.entity.Role;
import com.vsl.identity.entity.User;
import com.vsl.identity.entity.UserStatus;
import com.vsl.identity.repository.UserRepository;
import com.vsl.identity.service.AuthService;
import com.vsl.identity.service.EmailService;
import com.vsl.identity.service.OtpService;
import com.vsl.identity.service.RefreshTokenService;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final RefreshTokenService refreshTokenService;
    private final OtpService otpService;
    private final EmailService emailService;

    @Override
    public void sendRegisterOtp(SendRegisterOtpRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Email đã tồn tại");
        }
        if (userRepository.existsByUsername(request.getUsername().trim())) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Tên đăng nhập đã tồn tại");
        }

        String otp = otpService.generateAndStoreOtp(email);
        emailService.sendOtpEmail(email, otp);
        log.info("Đã gửi OTP đăng ký tới email={}", email);
    }

    @Override
    @Transactional
    public TokenResponse register(RegisterRequest request, String ipAddress, String deviceInfo) {
        String email = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByUsername(request.getUsername().trim())) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Tên đăng nhập đã tồn tại");
        }

        if (userRepository.existsByEmail(email)) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Email đã tồn tại");
        }

        if (!otpService.verifyOtp(email, request.getOtp())) {
            throw new AppException(ErrorCode.INVALID_OTP);
        }

        User user = User.builder()
                .username(request.getUsername().trim())
                .email(email)
                .fullName(request.getFullName().trim())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .build();

        user = userRepository.save(user);
        otpService.clearOtp(email);
        log.info("Đã tạo tài khoản mới: username={}, userId={}", user.getUsername(), user.getUserId());

        return generateTokenResponse(user, ipAddress, deviceInfo);
    }

    @Override
    @Transactional
    public TokenResponse login(LoginRequest request, String ipAddress, String deviceInfo) {
        String loginKey = request.getUsername().trim();

        User user = userRepository.findByUsernameOrEmail(loginKey)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_CREDENTIALS, "Tên đăng nhập/email hoặc mật khẩu không chính xác"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new AppException(ErrorCode.INVALID_CREDENTIALS, "Tên đăng nhập/email hoặc mật khẩu không chính xác");
        }

        if (!user.isActive()) {
            throw new AppException(ErrorCode.FORBIDDEN, "Tài khoản của bạn đã bị khóa. Vui lòng liên hệ quản trị viên.");
        }

        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        log.info("Người dùng đăng nhập thành công: username={}, userId={}", user.getUsername(), user.getUserId());
        return generateTokenResponse(user, ipAddress, deviceInfo);
    }

    @Override
    @Transactional
    public TokenResponse refreshToken(RefreshTokenRequest request) {
        RotationResult rotationResult = refreshTokenService.rotate(request.getRefreshToken());

        User user = userRepository.findById(rotationResult.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Không tìm thấy người dùng của phiên đăng nhập này"));

        if (!user.isActive()) {
            throw new AppException(ErrorCode.FORBIDDEN, "Tài khoản của bạn đã bị khóa.");
        }

        String newAccessToken = jwtService.generateAccessToken(
                user.getUserId(),
                user.getUsername(),
                user.getRole().name()
        );

        long expiresInSeconds = jwtProperties.getAccessTokenTtlMinutes() * 60;

        return TokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(rotationResult.getNewRawToken())
                .tokenType("Bearer")
                .expiresIn(expiresInSeconds)
                .userId(user.getUserId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }

    @Override
    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            refreshTokenService.revoke(refreshToken);
            log.info("Đã thu hồi refresh token khi đăng xuất.");
        }
    }

    @Override
    public void forgotPassword(ForgotPasswordRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Không tìm thấy tài khoản với email này"));

        String otp = otpService.generateAndStoreOtp(user.getEmail());
        emailService.sendOtpEmail(user.getEmail(), otp);
        log.info("Đã gửi OTP đặt lại mật khẩu tới email={}", user.getEmail());
    }

    @Override
    public void verifyOtp(VerifyOtpRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        if (!otpService.verifyOtp(email, request.getOtp())) {
            throw new AppException(ErrorCode.INVALID_OTP);
        }
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        if (!otpService.verifyOtp(email, request.getOtp())) {
            throw new AppException(ErrorCode.INVALID_OTP);
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Không tìm thấy tài khoản với email này"));

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        otpService.clearOtp(email);
        log.info("Đã đặt lại mật khẩu cho userId={}", user.getUserId());
    }

    private TokenResponse generateTokenResponse(User user, String ipAddress, String deviceInfo) {
        String accessToken = jwtService.generateAccessToken(
                user.getUserId(),
                user.getUsername(),
                user.getRole().name()
        );

        String rawRefreshToken = refreshTokenService.issue(user.getUserId(), ipAddress, deviceInfo);
        long expiresInSeconds = jwtProperties.getAccessTokenTtlMinutes() * 60;

        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(rawRefreshToken)
                .tokenType("Bearer")
                .expiresIn(expiresInSeconds)
                .userId(user.getUserId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }
}
