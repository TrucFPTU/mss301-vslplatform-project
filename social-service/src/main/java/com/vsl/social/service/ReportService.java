package com.vsl.social.service;

import com.vsl.social.dto.ReportRequest;

public interface ReportService {

    void report(Long blogId, Long userId, ReportRequest req);
}
