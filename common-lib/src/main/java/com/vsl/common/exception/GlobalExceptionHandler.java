package com.vsl.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.stream.Collectors;

/**
 * Xử lý exception tập trung, trả ErrorResponse thống nhất cho toàn hệ thống.
 * Import vào mỗi service qua @Import hoặc component-scan package com.vsl.common.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ErrorResponse> handleApp(AppException ex, HttpServletRequest req) {
        ErrorCode ec = ex.getErrorCode();
        return ResponseEntity.status(ec.getHttpStatus()).body(
                ErrorResponse.builder()
                        .status(ec.getHttpStatus().value())
                        .code(ec.getCode())
                        .error(ec.name())
                        .message(ex.getMessage())
                        .path(req.getRequestURI())
                        .timestamp(Instant.now())
                        .build());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("; "));
        ErrorCode ec = ErrorCode.VALIDATION_ERROR;
        return ResponseEntity.status(ec.getHttpStatus()).body(
                ErrorResponse.builder()
                        .status(ec.getHttpStatus().value())
                        .code(ec.getCode())
                        .error(ec.name())
                        .message(detail.isBlank() ? ec.getDefaultMessage() : detail)
                        .path(req.getRequestURI())
                        .timestamp(Instant.now())
                        .build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest req) {
        log.error("Unhandled exception at {}: {}", req.getRequestURI(), ex.getMessage(), ex);
        ErrorCode ec = ErrorCode.INTERNAL_ERROR;
        return ResponseEntity.status(ec.getHttpStatus()).body(
                ErrorResponse.builder()
                        .status(ec.getHttpStatus().value())
                        .code(ec.getCode())
                        .error(ec.name())
                        .message(ec.getDefaultMessage())
                        .path(req.getRequestURI())
                        .timestamp(Instant.now())
                        .build());
    }

    private String formatFieldError(FieldError fe) {
        return fe.getField() + ": " + fe.getDefaultMessage();
    }
}
