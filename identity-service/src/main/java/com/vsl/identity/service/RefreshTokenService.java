package com.vsl.identity.service;

import com.vsl.identity.dto.RotationResult;

public interface RefreshTokenService {

    String issue(Long userId, String ipAddress, String deviceInfo);

    RotationResult rotate(String rawToken);

    void revoke(String rawToken);

    void revokeAll(Long userId);
}
