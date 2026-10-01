package com.vsl.social.service;

import com.vsl.common.response.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.function.Function;

/** Tạo Pageable an toàn (giới hạn size) và đổi Page của Spring sang PageResponse dùng chung. */
final class Paging {

    static final int MAX_SIZE = 50;

    private Paging() {
    }

    static Pageable newestFirst(int page, int size) {
        return of(page, size, Sort.Direction.DESC);
    }

    static Pageable oldestFirst(int page, int size) {
        return of(page, size, Sort.Direction.ASC);
    }

    static <E, T> PageResponse<T> toResponse(Page<E> page, Function<E, T> mapper) {
        return PageResponse.of(page.map(mapper).getContent(), page.getNumber(), page.getSize(), page.getTotalElements());
    }

    private static Pageable of(int page, int size, Sort.Direction direction) {
        int safeSize = Math.min(Math.max(size, 1), MAX_SIZE);
        return PageRequest.of(Math.max(page, 0), safeSize, Sort.by(direction, "createdAt"));
    }
}
