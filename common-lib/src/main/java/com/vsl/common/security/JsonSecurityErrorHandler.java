package com.vsl.common.security;

import com.vsl.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

/**
 * Trả lỗi bảo mật ở tầng filter dưới dạng JSON chuẩn (cùng format ErrorResponse):
 * - chưa đăng nhập / token sai -> 401 UNAUTHENTICATED
 * - đã đăng nhập nhưng thiếu quyền -> 403 FORBIDDEN
 * Thiếu cấu hình này Spring Security mặc định trả 403 body rỗng cho cả hai trường hợp.
 * Gắn vào SecurityConfig: {@code .exceptionHandling(e -> e.authenticationEntryPoint(h).accessDeniedHandler(h))}.
 */
public class JsonSecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        write(response, request, ErrorCode.UNAUTHENTICATED);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        write(response, request, ErrorCode.FORBIDDEN);
    }

    private void write(HttpServletResponse response, HttpServletRequest request, ErrorCode ec) throws IOException {
        response.setStatus(ec.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        // JSON viết tay (5 trường cố định) để không phụ thuộc phiên bản Jackson của từng service.
        response.getWriter().write("{\"status\":" + ec.getHttpStatus().value()
                + ",\"code\":\"" + ec.getCode() + "\""
                + ",\"error\":\"" + ec.name() + "\""
                + ",\"message\":\"" + escape(ec.getDefaultMessage()) + "\""
                + ",\"path\":\"" + escape(request.getRequestURI()) + "\""
                + ",\"timestamp\":\"" + Instant.now() + "\"}");
    }

    private static String escape(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length());
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
                }
            }
        }
        return sb.toString();
    }
}
