package com.vsl.learning.service;

import com.vsl.learning.dto.response.PublicStatsResponse;

public interface PublicStatsService {

    /** Số liệu trang chủ: tổng người dùng, tổng từ vựng, tỉ lệ hài lòng. */
    PublicStatsResponse getLandingStats();
}
