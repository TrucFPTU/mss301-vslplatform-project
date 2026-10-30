package com.vsl.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.stream.Collectors;

/**
 * Xử lý exception tập trung, trả ErrorResponse thống nhất cho toàn hệ thống.
 * <p>
 * Lỗi phía client (sai tham số, JSON hỏng, sai method, URL không tồn tại, file quá lớn...) trả đúng
 * 4xx thay vì rơi xuống handler bắt-tất (500) như trước. Lỗi phân quyền / DB nằm ở
 * {@link SecurityExceptionHandler} / {@link DataExceptionHandler} (chỉ nạp khi có thư viện tương ứng).
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ErrorResponse> handleApp(AppException ex, HttpServletRequest req) {
        return ErrorResponses.of(ex.getErrorCode(), ex.getMessage(), req);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("; "));
        return ErrorResponses.of(ErrorCode.VALIDATION_ERROR, detail, req);
    }

    /** @Valid / ràng buộc trên @RequestParam, @PathVariable (Spring 6.1+). */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleMethodValidation(HandlerMethodValidationException ex, HttpServletRequest req) {
        String detail = ex.getAllErrors().stream()
                .map(e -> e.getDefaultMessage() == null ? "" : e.getDefaultMessage())
                .filter(m -> !m.isBlank())
                .collect(Collectors.joining("; "));
        return ErrorResponses.of(ErrorCode.VALIDATION_ERROR, detail, req);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraint(ConstraintViolationException ex, HttpServletRequest req) {
        String detail = ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining("; "));
        return ErrorResponses.of(ErrorCode.VALIDATION_ERROR, detail, req);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException ex, HttpServletRequest req) {
        return ErrorResponses.of(ErrorCode.VALIDATION_ERROR, "Thiếu tham số bắt buộc: " + ex.getParameterName(), req);
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ErrorResponse> handleMissingPart(MissingServletRequestPartException ex, HttpServletRequest req) {
        return ErrorResponses.of(ErrorCode.VALIDATION_ERROR, "Thiếu phần dữ liệu bắt buộc: " + ex.getRequestPartName(), req);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handleMissingHeader(MissingRequestHeaderException ex, HttpServletRequest req) {
        return ErrorResponses.of(ErrorCode.VALIDATION_ERROR, "Thiếu header bắt buộc: " + ex.getHeaderName(), req);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest req) {
        return ErrorResponses.of(ErrorCode.VALIDATION_ERROR,
                "Giá trị không hợp lệ cho tham số '" + ex.getName() + "': " + ex.getValue(), req);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException ex, HttpServletRequest req) {
        return ErrorResponses.of(ErrorCode.VALIDATION_ERROR, "Nội dung request không đúng định dạng JSON", req);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethod(HttpRequestMethodNotSupportedException ex, HttpServletRequest req) {
        return ErrorResponses.of(ErrorCode.METHOD_NOT_ALLOWED,
                "Phương thức " + ex.getMethod() + " không được hỗ trợ cho đường dẫn này", req);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMediaType(HttpMediaTypeNotSupportedException ex, HttpServletRequest req) {
        return ErrorResponses.of(ErrorCode.UNSUPPORTED_MEDIA_TYPE, null, req);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleTooLarge(MaxUploadSizeExceededException ex, HttpServletRequest req) {
        return ErrorResponses.of(ErrorCode.PAYLOAD_TOO_LARGE, null, req);
    }

    /** Xử lý bất đồng bộ quá thời gian chờ (vd suy luận AI) -> 503 "thử lại" thay vì 500. */
    @ExceptionHandler(AsyncRequestTimeoutException.class)
    public ResponseEntity<ErrorResponse> handleAsyncTimeout(AsyncRequestTimeoutException ex, HttpServletRequest req) {
        return ErrorResponses.of(ErrorCode.AI_SERVICE_BUSY, "Xử lý quá thời gian chờ, vui lòng thử lại sau", req);
    }

    /** URL không tồn tại -> 404 (trước đây bị biến thành 500). */
    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ResponseEntity<ErrorResponse> handleNotFound(Exception ex, HttpServletRequest req) {
        return ErrorResponses.of(ErrorCode.NOT_FOUND, "Không tìm thấy đường dẫn " + req.getRequestURI(), req);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest req) {
        log.error("Unhandled exception at {}: {}", req.getRequestURI(), ex.getMessage(), ex);
        return ErrorResponses.of(ErrorCode.INTERNAL_ERROR, null, req);
    }

    private String formatFieldError(FieldError fe) {
        return fe.getField() + ": " + fe.getDefaultMessage();
    }

    /** Dựng body lỗi chuẩn - dùng chung cho các advice và cho entry point của Spring Security. */
    public static final class ErrorResponses {

        private ErrorResponses() {
        }

        public static ResponseEntity<ErrorResponse> of(ErrorCode ec, String message, HttpServletRequest req) {
            return ResponseEntity.status(ec.getHttpStatus()).body(body(ec, message, req.getRequestURI()));
        }

        public static ErrorResponse body(ErrorCode ec, String message, String path) {
            return ErrorResponse.builder()
                    .status(ec.getHttpStatus().value())
                    .code(ec.getCode())
                    .error(ec.name())
                    .message(message == null || message.isBlank() ? ec.getDefaultMessage() : message)
                    .path(path)
                    .timestamp(Instant.now())
                    .build();
        }
    }
}
