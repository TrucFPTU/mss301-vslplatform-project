package com.vsl.identity.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vsl.common.exception.AppException;
import com.vsl.common.exception.ErrorCode;
import com.vsl.common.exception.GlobalExceptionHandler;
import com.vsl.identity.dto.LoginRequest;
import com.vsl.identity.dto.RefreshTokenRequest;
import com.vsl.identity.dto.RegisterRequest;
import com.vsl.identity.dto.TokenResponse;
import com.vsl.identity.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("API POST /api/auth/register thành công trả về 200 và ApiResponse")
    void register_Success() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .username("testuser")
                .email("test@example.com")
                .password("password123")
                .fullName("Test User")
                .otp("123456")
                .build();

        TokenResponse tokenResponse = TokenResponse.builder()
                .accessToken("access_token_123")
                .refreshToken("refresh_token_123")
                .tokenType("Bearer")
                .expiresIn(1800L)
                .userId(1L)
                .username("testuser")
                .email("test@example.com")
                .role("USER")
                .build();

        when(authService.register(any(), any(), any())).thenReturn(tokenResponse);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.accessToken").value("access_token_123"))
                .andExpect(jsonPath("$.data.username").value("testuser"));
    }

    @Test
    @DisplayName("API POST /api/auth/register trả về 400 khi dữ liệu rỗng")
    void register_ValidationError() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .username("")
                .email("invalid-email")
                .password("123")
                .fullName("")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("SYS_0002"));
    }

    @Test
    @DisplayName("API POST /api/auth/login thành công")
    void login_Success() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .username("testuser")
                .password("password123")
                .build();

        TokenResponse tokenResponse = TokenResponse.builder()
                .accessToken("access_token_123")
                .refreshToken("refresh_token_123")
                .tokenType("Bearer")
                .expiresIn(1800L)
                .userId(1L)
                .username("testuser")
                .role("USER")
                .build();

        when(authService.login(any(), any(), any())).thenReturn(tokenResponse);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("access_token_123"));
    }

    @Test
    @DisplayName("API POST /api/auth/login ném AppException khi sai mật khẩu")
    void login_InvalidCredentials() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .username("testuser")
                .password("wrongpassword")
                .build();

        when(authService.login(any(), any(), any()))
                .thenThrow(new AppException(ErrorCode.INVALID_CREDENTIALS, "Tên đăng nhập/email hoặc mật khẩu không chính xác"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_1003"));
    }

    @Test
    @DisplayName("API POST /api/auth/refresh thành công")
    void refresh_Success() throws Exception {
        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("valid_refresh_token")
                .build();

        TokenResponse tokenResponse = TokenResponse.builder()
                .accessToken("new_access_token")
                .refreshToken("new_refresh_token")
                .tokenType("Bearer")
                .expiresIn(1800L)
                .userId(1L)
                .username("testuser")
                .role("USER")
                .build();

        when(authService.refreshToken(any())).thenReturn(tokenResponse);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("new_access_token"));
    }

    @Test
    @DisplayName("API POST /api/auth/logout thành công")
    void logout_Success() throws Exception {
        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("valid_refresh_token")
                .build();

        doNothing().when(authService).logout(anyString());

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Đăng xuất thành công"));
    }
}
