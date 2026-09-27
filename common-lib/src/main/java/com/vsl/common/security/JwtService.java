package com.vsl.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/**
 * Ký & kiểm tra JWT (thuật toán HS256, khóa bí mật đối xứng dùng chung).
 *
 * - identity-service dùng {@link #generateAccessToken} để PHÁT token khi đăng nhập.
 * - Mọi service dùng {@link #parse} để KIỂM TRA token đến (chữ ký + hạn dùng).
 *
 * Claims chứa trong token (hợp đồng chung — mọi service đọc giống nhau):
 *   sub      = userId (Long, dạng chuỗi)
 *   username = tên đăng nhập
 *   role     = vai trò (USER / ADMIN ...)
 */
public class JwtService {

    private final SecretKey key;
    private final JwtProperties props;

    public JwtService(JwtProperties props) {
        this.props = props;
        // Giải mã secret Base64 thành khóa HS256. Secret phải ≥ 256-bit (32 byte) sau khi decode.
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(props.getSecret()));
    }

    /** Phát access token cho một user (gọi bởi identity-service khi login). */
    public String generateAccessToken(Long userId, String username, String role) {
        Instant now = Instant.now();
        Instant exp = now.plus(props.getAccessTokenTtlMinutes(), ChronoUnit.MINUTES);
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .claim("role", role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(key)      // HS256 tự suy ra từ loại khóa
                .compact();
    }

    /**
     * Kiểm tra & giải mã token. Ném exception nếu chữ ký sai hoặc token hết hạn.
     * Trả về phần payload để đọc claims (userId, role...).
     */
    public Jws<Claims> parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token);
    }

    /** Tiện ích: lấy userId từ token đã hợp lệ. */
    public Long extractUserId(String token) {
        return Long.valueOf(parse(token).getPayload().getSubject());
    }
}
