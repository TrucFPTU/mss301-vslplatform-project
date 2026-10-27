package com.vsl.learning.controller;

import com.vsl.common.response.ApiResponse;
import com.vsl.common.response.PageResponse;
import com.vsl.learning.dto.response.AdminSummaryResponse;
import com.vsl.learning.dto.response.VisitLogResponse;
import com.vsl.learning.dto.response.VisitTimePoint;
import com.vsl.learning.service.VisitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/** Dashboard admin: tổng quan + lượt truy cập. (Bản gốc: /api/admin/analytics.) */
@Tag(name = "Admin Visit Analytics", description = "Tổng quan hệ thống và lượt truy cập")
@RestController
@RequestMapping("/api/visits/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminVisitAnalyticsController {

    private final VisitService visitService;

    @Operation(summary = "4 chỉ số tổng quan: học viên, lượt truy cập, từ vựng, blog")
    @GetMapping("/summary")
    public ApiResponse<AdminSummaryResponse> getSummary() {
        return ApiResponse.ok(visitService.getSummary());
    }

    @Operation(summary = "Biểu đồ lượt truy cập theo DAY/MONTH/YEAR")
    @GetMapping("/timeseries")
    public ApiResponse<List<VisitTimePoint>> getTimeSeries(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "DAY") String granularity) {
        return ApiResponse.ok(visitService.getVisitTimeSeries(from, to, granularity));
    }

    @Operation(summary = "Bảng log truy cập chi tiết (mới nhất trước)")
    @GetMapping("/logs")
    public ApiResponse<PageResponse<VisitLogResponse>> getVisitLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(visitService.getVisitLogs(page, size));
    }
}
