package com.vsl.learning.dto.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Bản sao phía client của ApiResponse/ErrorResponse (common-lib) để đọc JSON trả về từ ai-service.
 * ApiResponse của common-lib không có constructor rỗng nên không dùng trực tiếp để deserialize.
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiServiceResponse<T> {
    private boolean success;
    private String code;
    private String message;
    private T data;
}
