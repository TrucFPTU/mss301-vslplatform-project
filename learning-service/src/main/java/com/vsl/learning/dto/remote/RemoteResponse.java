package com.vsl.learning.dto.remote;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Envelope ApiResponse của service khác, chỉ lấy phần cần đọc.
 * (ApiResponse của common-lib không có constructor rỗng nên không dùng để deserialize.)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RemoteResponse<T>(boolean success, String message, T data) {
}
