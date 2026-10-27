package com.vsl.learning.service;

import com.vsl.common.response.PageResponse;
import com.vsl.learning.dto.response.AdminSummaryResponse;
import com.vsl.learning.dto.response.VisitLogResponse;
import com.vsl.learning.dto.response.VisitTimePoint;

import java.time.LocalDate;
import java.util.List;

public interface VisitService {

    /**
     * Ghi 1 lượt truy cập (chạy nền). userId null = khách.
     * sessionId (nếu có): upsert theo phiên - phiên đã có dòng thì gắn user vào dòng đó thay vì tạo mới.
     */
    void recordVisit(Long userId, String sessionId, String ipAddress, String userAgent);

    /** 4 chỉ số tổng quan (học viên, lượt truy cập, từ vựng, blog đã xuất bản). */
    AdminSummaryResponse getSummary();

    /** Chuỗi thời gian lượt truy cập theo DAY/MONTH/YEAR, đã lấp đầy khoảng trống. */
    List<VisitTimePoint> getVisitTimeSeries(LocalDate from, LocalDate to, String granularity);

    /** Bảng log truy cập chi tiết, phân trang (mới nhất trước). */
    PageResponse<VisitLogResponse> getVisitLogs(int page, int size);
}
