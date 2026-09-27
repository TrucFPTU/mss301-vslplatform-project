package com.vsl.common.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Bộ lọc chạy 1 lần/request: đọc header "Authorization: Bearer <token>",
 * kiểm tra token, và nếu hợp lệ thì nạp thông tin user vào SecurityContext.
 *
 * Sau khi lọc:
 *   - principal   = userId (Long)  → controller lấy qua @AuthenticationPrincipal Long userId
 *   - authorities = ["ROLE_<role>"] → dùng cho @PreAuthorize("hasRole('ADMIN')") ...
 *
 * Token thiếu/sai → KHÔNG chặn ở đây, chỉ để request đi tiếp mà chưa xác thực;
 * SecurityFilterChain của từng service sẽ tự trả 401 cho các route cần đăng nhập.
 */
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(HEADER);
        if (header != null && header.startsWith(PREFIX)) {
            String token = header.substring(PREFIX.length());
            try {
                Claims claims = jwtService.parse(token).getPayload();
                Long userId = Long.valueOf(claims.getSubject());
                String role = claims.get("role", String.class);

                var authorities = role == null
                        ? List.<SimpleGrantedAuthority>of()
                        : List.of(new SimpleGrantedAuthority("ROLE_" + role));

                var authentication = new UsernamePasswordAuthenticationToken(userId, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (Exception e) {
                // Token sai/hết hạn: bỏ qua, để nguyên trạng thái chưa xác thực.
                log.debug("Bỏ qua token không hợp lệ: {}", e.getMessage());
            }
        }
        filterChain.doFilter(request, response);
    }
}
