package com.vsl.learning.dto.response;

import java.time.LocalDateTime;

/**
 * Một dòng trong bảng log truy cập. Tên/avatar người dùng do identity-service quản lý nên
 * chỉ trả userId (frontend tự tra nếu cần hiển thị).
 */
public record VisitLogResponse(
        Long id,
        String visitorType,   // "USER" hoặc "GUEST"
        Long userId,          // null nếu khách
        String deviceInfo,
        String ipAddress,
        String location,
        LocalDateTime visitedAt
) {
}
