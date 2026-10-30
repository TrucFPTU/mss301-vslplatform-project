package com.vsl.social.service;

import com.vsl.common.response.PageResponse;
import com.vsl.social.dto.ReportDetailResponse;
import com.vsl.social.dto.ReportRequest;
import com.vsl.social.dto.ReportResponse;
import com.vsl.social.entity.ReportStatus;

public interface ReportService {

    void report(Long blogId, Long userId, ReportRequest req);

    PageResponse<ReportResponse> listReports(ReportStatus status, int page, int size);

    ReportDetailResponse getReportDetail(Long id);

    long countPending();

    /** Tố cáo đúng: gỡ bài với lý do (hiển thị cho tác giả) và đóng MỌI tố cáo đang chờ của bài. */
    ReportResponse resolve(Long id, Long adminId, String reason);

    /** Tố cáo sai: chỉ đóng tố cáo này, bài giữ nguyên. */
    ReportResponse reject(Long id);
}
