package com.vsl.common.exception;

import lombok.Getter;

/**
 * Exception nghiệp vụ dùng chung. Ném ở bất kỳ service nào,
 * GlobalExceptionHandler sẽ dịch thành ErrorResponse theo ErrorCode.
 */
@Getter
public class AppException extends RuntimeException {

    private final ErrorCode errorCode;

    public AppException(ErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
    }

    public AppException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
