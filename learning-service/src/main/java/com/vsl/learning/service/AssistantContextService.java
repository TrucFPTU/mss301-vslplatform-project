package com.vsl.learning.service;

import com.vsl.learning.dto.response.AssistantContextResponse;

public interface AssistantContextService {

    /** Tổng quan hệ thống + tiến độ + gợi ý bài tiếp theo của {@code userId} cho trợ lý học tập. */
    AssistantContextResponse getContext(Long userId);
}
