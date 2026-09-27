package com.vsl.common.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Ánh xạ cấu hình JWT từ file yml (prefix "security.jwt").
 * Giá trị lấy từ config-server → config-repo/application.yml (dùng chung mọi service).
 *
 * <pre>
 * security:
 *   jwt:
 *     secret: ...            # chuỗi Base64 ≥ 256-bit
 *     access-token-ttl-minutes: 30
 *     refresh-token-ttl-days: 7
 * </pre>
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "security.jwt")
public class JwtProperties {
    /** Khóa bí mật HS256 (Base64). identity-service ký, mọi service verify bằng khóa này. */
    private String secret;
    /** Thời gian sống của access token (phút). */
    private long accessTokenTtlMinutes = 30;
    /** Thời gian sống của refresh token (ngày). */
    private long refreshTokenTtlDays = 7;
}
