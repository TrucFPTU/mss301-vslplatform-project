package com.vsl.identity.service;

import com.vsl.identity.dto.LoginRequest;
import com.vsl.identity.dto.RefreshTokenRequest;
import com.vsl.identity.dto.RegisterRequest;
import com.vsl.identity.dto.TokenResponse;

public interface AuthService {

    TokenResponse register(RegisterRequest request, String ipAddress, String deviceInfo);

    TokenResponse login(LoginRequest request, String ipAddress, String deviceInfo);

    TokenResponse refreshToken(RefreshTokenRequest request);

    void logout(String refreshToken);
}
