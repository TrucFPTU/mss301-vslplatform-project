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

    ReportResponse resolve(Long id);

    ReportResponse reject(Long id);
}
