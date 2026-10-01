package com.vsl.learning.service;

import com.vsl.common.response.PageResponse;
import org.springframework.data.domain.Page;

/** Tiện ích phân trang: chặn tham số xấu và đổi Page (Spring Data) sang PageResponse (common-lib). */
final class Paging {

    static final int MAX_PAGE_SIZE = 100;

    private Paging() {
    }

    static int safePage(int page) {
        return Math.max(page, 0);
    }

    static int safeSize(int size) {
        return clamp(size, 1, MAX_PAGE_SIZE);
    }

    static int clamp(int value, int min, int max) {
        return Math.min(Math.max(value, min), max);
    }

    static <T> PageResponse<T> toPageResponse(Page<T> page) {
        return PageResponse.of(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements());
    }
}
