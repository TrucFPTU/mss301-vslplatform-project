package com.vsl.learning.controller;

import com.vsl.learning.service.VisitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Ghi nhận lượt truy cập hệ thống (cả khách lẫn học viên).
 * Công khai: frontend gửi 1 beacon mỗi phiên khi mở app. Có token thì gắn userId.
 */
@Tag(name = "Visit", description = "Ghi nhận lượt truy cập")
@RestController
@RequestMapping("/api/visits")
@RequiredArgsConstructor
public class VisitController {

    private final VisitService visitService;

    @Operation(summary = "Ghi 1 lượt truy cập (beacon, trả 202 ngay)")
    @PostMapping("/track")
    public ResponseEntity<Void> track(
            @AuthenticationPrincipal Long userId,
            @RequestHeader(value = "X-Visit-Session", required = false) String sessionId,
            HttpServletRequest request) {
        // Ghi nền (async) rồi trả 202 ngay -> không làm chậm page load.
        visitService.recordVisit(userId, sessionId, resolveClientIp(request), request.getHeader("User-Agent"));
        return ResponseEntity.accepted().build();
    }

    /** Lấy IP thật khi đứng sau gateway/proxy (X-Forwarded-For). */
    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }
}
