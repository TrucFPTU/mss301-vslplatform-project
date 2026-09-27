package com.vsl.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Danh mục mã lỗi dùng chung toàn hệ thống.
 * Quy ước code: {DOMAIN}_{4 số}. Mỗi service có thể bổ sung mã riêng theo cùng quy ước.
 */
@Getter
public enum ErrorCode {

    // ---- Chung ----
    INTERNAL_ERROR("SYS_0001", "Lỗi hệ thống", HttpStatus.INTERNAL_SERVER_ERROR),
    VALIDATION_ERROR("SYS_0002", "Dữ liệu không hợp lệ", HttpStatus.BAD_REQUEST),
    NOT_FOUND("SYS_0003", "Không tìm thấy tài nguyên", HttpStatus.NOT_FOUND),
    FORBIDDEN("SYS_0004", "Không có quyền truy cập", HttpStatus.FORBIDDEN),

    // ---- Auth / Identity ----
    UNAUTHENTICATED("AUTH_1001", "Chưa xác thực", HttpStatus.UNAUTHORIZED),
    INVALID_TOKEN("AUTH_1002", "Token không hợp lệ hoặc đã hết hạn", HttpStatus.UNAUTHORIZED),
    INVALID_CREDENTIALS("AUTH_1003", "Sai thông tin đăng nhập", HttpStatus.UNAUTHORIZED),

    // ---- AI ----
    AI_MODEL_NOT_LOADED("AI_3001", "Model AI chưa sẵn sàng", HttpStatus.SERVICE_UNAVAILABLE),
    AI_INFERENCE_ERROR("AI_3002", "Lỗi khi xử lý video", HttpStatus.INTERNAL_SERVER_ERROR),
    VIDEO_EMPTY("AI_3003", "Video rỗng", HttpStatus.BAD_REQUEST),
    VIDEO_INVALID_TYPE("AI_3004", "Định dạng video không hợp lệ", HttpStatus.BAD_REQUEST);

    private final String code;
    private final String defaultMessage;
    private final HttpStatus httpStatus;

    ErrorCode(String code, String defaultMessage, HttpStatus httpStatus) {
        this.code = code;
        this.defaultMessage = defaultMessage;
        this.httpStatus = httpStatus;
    }
}
