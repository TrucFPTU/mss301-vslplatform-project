package com.vsl.identity.service;

import com.vsl.common.exception.AppException;
import com.vsl.common.security.JwtProperties;
import com.vsl.common.security.JwtService;
import com.vsl.identity.dto.LoginRequest;
import com.vsl.identity.dto.RefreshTokenRequest;
import com.vsl.identity.dto.RegisterRequest;
import com.vsl.identity.dto.RotationResult;
import com.vsl.identity.dto.TokenResponse;
import com.vsl.identity.entity.Role;
import com.vsl.identity.entity.User;
import com.vsl.identity.entity.UserStatus;
import com.vsl.identity.repository.UserRepository;
import com.vsl.identity.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private JwtProperties jwtProperties;

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private AuthServiceImpl authService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .userId(1L)
                .username("testuser")
                .email("test@example.com")
                .fullName("Test User")
                .passwordHash("hashed_password")
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .build();
    }

    @Test
    @DisplayName("Đăng ký thành công trả về TokenResponse")
    void register_Success() {
        RegisterRequest request = RegisterRequest.builder()
                .username("testuser")
                .email("test@example.com")
                .password("password123")
                .fullName("Test User")
                .build();

        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed_password");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);
        when(jwtProperties.getAccessTokenTtlMinutes()).thenReturn(30L);
        when(jwtService.generateAccessToken(eq(1L), eq("testuser"), eq("USER"))).thenReturn("mock_access_token");
        when(refreshTokenService.issue(eq(1L), any(), any())).thenReturn("mock_refresh_token");

        TokenResponse response = authService.register(request, "127.0.0.1", "Unit-Test");

        assertNotNull(response);
        assertEquals("mock_access_token", response.getAccessToken());
        assertEquals("mock_refresh_token", response.getRefreshToken());
        assertEquals("testuser", response.getUsername());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Đăng ký thất bại khi username đã tồn tại")
    void register_DuplicateUsername_ThrowsAppException() {
        RegisterRequest request = RegisterRequest.builder()
                .username("testuser")
                .email("test@example.com")
                .password("password123")
                .fullName("Test User")
                .build();

        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        assertThrows(AppException.class, () -> authService.register(request, "127.0.0.1", "Unit-Test"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Đăng nhập thành công trả về TokenResponse")
    void login_Success() {
        LoginRequest request = LoginRequest.builder()
                .username("testuser")
                .password("password123")
                .build();

        when(userRepository.findByUsernameOrEmail("testuser")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("password123", "hashed_password")).thenReturn(true);
        when(jwtProperties.getAccessTokenTtlMinutes()).thenReturn(30L);
        when(jwtService.generateAccessToken(eq(1L), eq("testuser"), eq("USER"))).thenReturn("mock_access_token");
        when(refreshTokenService.issue(eq(1L), any(), any())).thenReturn("mock_refresh_token");

        TokenResponse response = authService.login(request, "127.0.0.1", "Unit-Test");

        assertNotNull(response);
        assertEquals("mock_access_token", response.getAccessToken());
        assertEquals(1L, response.getUserId());
    }

    @Test
    @DisplayName("Đăng nhập thất bại khi sai mật khẩu")
    void login_WrongPassword_ThrowsAppException() {
        LoginRequest request = LoginRequest.builder()
                .username("testuser")
                .password("wrongpassword")
                .build();

        when(userRepository.findByUsernameOrEmail("testuser")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("wrongpassword", "hashed_password")).thenReturn(false);

        assertThrows(AppException.class, () -> authService.login(request, "127.0.0.1", "Unit-Test"));
    }

    @Test
    @DisplayName("Làm mới token thành công khi refresh token hợp lệ")
    void refreshToken_Success() {
        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("valid_refresh_token")
                .build();

        when(refreshTokenService.rotate("valid_refresh_token"))
                .thenReturn(new RotationResult(1L, "new_refresh_token"));
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(jwtProperties.getAccessTokenTtlMinutes()).thenReturn(30L);
        when(jwtService.generateAccessToken(eq(1L), eq("testuser"), eq("USER"))).thenReturn("new_access_token");

        TokenResponse response = authService.refreshToken(request);

        assertNotNull(response);
        assertEquals("new_access_token", response.getAccessToken());
        assertEquals("new_refresh_token", response.getRefreshToken());
    }
}
