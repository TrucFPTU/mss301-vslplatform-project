package com.vsl.social.controller;

import com.vsl.common.response.ApiResponse;
import com.vsl.common.response.PageResponse;
import com.vsl.social.dto.HideBlogRequest;
import com.vsl.social.dto.ReportDetailResponse;
import com.vsl.social.dto.ReportResponse;
import com.vsl.social.entity.ReportStatus;
import com.vsl.social.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Admin Blog Reports", description = "API Quản trị & Duyệt Báo cáo Vi phạm Bài viết (Dành cho ADMIN)")
@RestController
@RequestMapping("/api/blogs/admin/reports")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class BlogReportAdminController {

    private final ReportService reportService;

    @Operation(summary = "Danh sách báo cáo vi phạm",
            description = "Lấy danh sách các báo cáo vi phạm bài viết (phân trang, lọc theo status: PENDING, RESOLVED, REJECTED).")
    @GetMapping
    public ApiResponse<PageResponse<ReportResponse>> listReports(
            @Parameter(description = "Lọc theo trạng thái báo cáo (PENDING, RESOLVED, REJECTED)")
            @RequestParam(required = false) ReportStatus status,
            @Parameter(description = "Số trang (bắt đầu từ 0)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Số lượng bản ghi mỗi trang")
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(reportService.listReports(status, page, size));
    }

    @Operation(summary = "Chi tiết báo cáo vi phạm",
            description = "Xem chi tiết một báo cáo kèm toàn bộ nội dung của bài viết bị tố cáo.")
    @GetMapping("/{id}")
    public ApiResponse<ReportDetailResponse> getReportDetail(
            @Parameter(description = "ID của báo cáo vi phạm")
            @PathVariable Long id) {
        return ApiResponse.ok(reportService.getReportDetail(id));
    }

    @Operation(summary = "Số báo cáo đang chờ xử lý")
    @GetMapping("/pending-count")
    public ApiResponse<Long> pendingCount() {
        return ApiResponse.ok(reportService.countPending());
    }

    @Operation(summary = "Duyệt xử lý báo cáo vi phạm",
            description = "Báo cáo đúng: gỡ bài (HIDDEN) kèm lý do hiển thị cho tác giả và đóng MỌI báo cáo đang chờ của bài.")
    @PutMapping("/{id}/resolve")
    public ApiResponse<ReportResponse> resolve(
            @Parameter(description = "ID của báo cáo vi phạm")
            @PathVariable Long id,
            @AuthenticationPrincipal Long adminId,
            @Valid @RequestBody HideBlogRequest req) {
        return ApiResponse.ok(reportService.resolve(id, adminId, req.reason()), "Đã xử lý vi phạm và gỡ bài viết");
    }

    @Operation(summary = "Bác bỏ báo cáo vi phạm",
            description = "Admin bác bỏ báo cáo: chuyển trạng thái báo cáo sang REJECTED nếu bài viết không vi phạm (giữ nguyên bài viết).")
    @PutMapping("/{id}/reject")
    public ApiResponse<ReportResponse> reject(
            @Parameter(description = "ID của báo cáo vi phạm")
            @PathVariable Long id) {
        return ApiResponse.ok(reportService.reject(id), "Đã bác bỏ báo cáo vi phạm");
    }
}
