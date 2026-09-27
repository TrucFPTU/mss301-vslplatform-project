package com.vsl.common.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

/**
 * Body chuẩn khi có lỗi.
 * <pre>
 * { "status": 401, "code": "AUTH_1003", "error": "INVALID_CREDENTIALS",
 *   "message": "...", "path": "/api/...", "timestamp": "..." }
 * </pre>
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    private final int status;
    private final String code;
    private final String error;
    private final String message;
    private final String path;
    private final Instant timestamp;
}
