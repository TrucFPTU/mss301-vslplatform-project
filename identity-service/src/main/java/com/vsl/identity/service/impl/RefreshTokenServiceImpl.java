package com.vsl.identity.service.impl;

import com.vsl.common.exception.AppException;
import com.vsl.common.exception.ErrorCode;
import com.vsl.common.security.JwtProperties;
import com.vsl.identity.dto.RotationResult;
import com.vsl.identity.entity.RefreshToken;
import com.vsl.identity.repository.RefreshTokenRepository;
import com.vsl.identity.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;

    @Override
    @Transactional
    public String issue(Long userId, String ipAddress, String deviceInfo) {
        String rawToken = UUID.randomUUID().toString();
        String tokenHash = hash(rawToken);

        long ttlDays = jwtProperties.getRefreshTokenTtlDays() > 0 ? jwtProperties.getRefreshTokenTtlDays() : 7;

        RefreshToken token = RefreshToken.builder()
                .userId(userId)
                .tokenHash(tokenHash)
                .expiresAt(LocalDateTime.now().plus(ttlDays, ChronoUnit.DAYS))
                .revoked(false)
                .ipAddress(ipAddress)
                .deviceInfo(deviceInfo)
                .build();

        refreshTokenRepository.save(token);
        return rawToken;
    }

    @Override
    @Transactional
    public RotationResult rotate(String rawToken) {
        String tokenHash = hash(rawToken);

        RefreshToken existing = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_TOKEN, "Refresh token không tồn tại hoặc không hợp lệ"));

        if (existing.isRevoked()) {
            refreshTokenRepository.revokeAllByUserId(existing.getUserId());
            log.warn("Phát hiện refresh token đã bị thu hồi của user ID: {}. Đã thu hồi toàn bộ session.", existing.getUserId());
            throw new AppException(ErrorCode.UNAUTHENTICATED, "Refresh token đã được sử dụng. Tất cả các phiên đã bị thu hồi.");
        }

        if (existing.getExpiresAt().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(existing);
            throw new AppException(ErrorCode.INVALID_TOKEN, "Refresh token đã hết hạn. Vui lòng đăng nhập lại.");
        }

        // Thu hồi token cũ và sinh token mới
        existing.setRevoked(true);
        refreshTokenRepository.save(existing);

        String newRawToken = issue(existing.getUserId(), existing.getIpAddress(), existing.getDeviceInfo());
        return new RotationResult(existing.getUserId(), newRawToken);
    }

    @Override
    @Transactional
    public void revoke(String rawToken) {
        String tokenHash = hash(rawToken);
        refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        });
    }

    @Override
    @Transactional
    public void revokeAll(Long userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new AppException(ErrorCode.INTERNAL_ERROR, "Không thể khởi tạo mã băm SHA-256");
        }
    }
}
