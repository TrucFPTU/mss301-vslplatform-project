package com.vsl.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Lỗi phân quyền ném ra TRONG controller (vd @PreAuthorize("hasRole('ADMIN')")).
 * Trước đây rơi xuống handler bắt-tất -> 500. Giờ: chưa đăng nhập -> 401, đăng nhập nhưng thiếu quyền -> 403.
 * Ưu tiên cao hơn GlobalExceptionHandler để không bị handler Exception chặn trước.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SecurityExceptionHandler {

    @ExceptionHandler({AccessDeniedException.class, AuthorizationDeniedException.class})
    public ResponseEntity<ErrorResponse> handleDenied(RuntimeException ex, HttpServletRequest req) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean anonymous = auth == null || auth instanceof AnonymousAuthenticationToken || !auth.isAuthenticated();
        return GlobalExceptionHandler.ErrorResponses.of(
                anonymous ? ErrorCode.UNAUTHENTICATED : ErrorCode.FORBIDDEN, null, req);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException ex, HttpServletRequest req) {
        return GlobalExceptionHandler.ErrorResponses.of(ErrorCode.UNAUTHENTICATED, null, req);
    }
}
