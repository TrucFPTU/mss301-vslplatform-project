package com.vsl.identity.service;

import com.vsl.common.exception.AppException;
import com.vsl.common.security.JwtProperties;
import com.vsl.identity.dto.RotationResult;
import com.vsl.identity.entity.RefreshToken;
import com.vsl.identity.repository.RefreshTokenRepository;
import com.vsl.identity.service.impl.RefreshTokenServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtProperties jwtProperties;

    @InjectMocks
    private RefreshTokenServiceImpl refreshTokenService;

    @BeforeEach
    void setUp() {
        lenient().when(jwtProperties.getRefreshTokenTtlDays()).thenReturn(7L);
    }

    @Test
    @DisplayName("Cấp mới refresh token thành công")
    void issue_Success() {
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));

        String rawToken = refreshTokenService.issue(1L, "127.0.0.1", "Chrome");

        assertNotNull(rawToken);
        assertFalse(rawToken.isBlank());
        verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Xoay vòng token thành công khi token còn hạn và chưa bị thu hồi")
    void rotate_Success() {
        RefreshToken validToken = RefreshToken.builder()
                .id(10L)
                .userId(1L)
                .tokenHash("some_hash")
                .expiresAt(LocalDateTime.now().plusDays(5))
                .revoked(false)
                .build();

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(validToken));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));

        RotationResult result = refreshTokenService.rotate("raw_token_xyz");

        assertNotNull(result);
        assertEquals(1L, result.getUserId());
        assertNotNull(result.getNewRawToken());
        assertTrue(validToken.isRevoked()); // Token cũ phải bị đánh dấu thu hồi
    }

    @Test
    @DisplayName("Xoay vòng token ném lỗi và thu hồi toàn bộ session khi token đã bị revoke trước đó")
    void rotate_RevokedTokenReused_RevokesAllSessions() {
        RefreshToken revokedToken = RefreshToken.builder()
                .id(10L)
                .userId(1L)
                .tokenHash("some_hash")
                .expiresAt(LocalDateTime.now().plusDays(5))
                .revoked(true) // Đã bị revoke
                .build();

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(revokedToken));

        assertThrows(AppException.class, () -> refreshTokenService.rotate("reused_raw_token"));
        verify(refreshTokenRepository, times(1)).revokeAllByUserId(1L);
    }

    @Test
    @DisplayName("Xoay vòng token ném lỗi khi token đã hết hạn")
    void rotate_ExpiredToken() {
        RefreshToken expiredToken = RefreshToken.builder()
                .id(10L)
                .userId(1L)
                .tokenHash("some_hash")
                .expiresAt(LocalDateTime.now().minusMinutes(1)) // Đã hết hạn
                .revoked(false)
                .build();

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(expiredToken));

        assertThrows(AppException.class, () -> refreshTokenService.rotate("expired_raw_token"));
        verify(refreshTokenRepository, times(1)).delete(expiredToken);
    }
}
