package com.vsl.social.service;

import com.vsl.common.response.PageResponse;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PagingTest {

    @Test
    void newestFirst_sortsByCreatedAtDesc() {
        Pageable p = Paging.newestFirst(2, 10);
        assertThat(p.getPageNumber()).isEqualTo(2);
        assertThat(p.getPageSize()).isEqualTo(10);
        assertThat(p.getSort().getOrderFor("createdAt").getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void oldestFirst_sortsByCreatedAtAsc() {
        assertThat(Paging.oldestFirst(0, 10).getSort().getOrderFor("createdAt").getDirection())
                .isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void clampsPageAndSize() {
        assertThat(Paging.newestFirst(-5, 1000).getPageNumber()).isZero();
        assertThat(Paging.newestFirst(0, 1000).getPageSize()).isEqualTo(Paging.MAX_SIZE);
        assertThat(Paging.newestFirst(0, 0).getPageSize()).isEqualTo(1);
        assertThat(Paging.newestFirst(0, -3).getPageSize()).isEqualTo(1);
    }

    @Test
    void toResponse_mapsContentAndMetadata() {
        var page = new PageImpl<>(List.of(1, 2), PageRequest.of(0, 2), 5);
        PageResponse<String> res = Paging.toResponse(page, i -> "#" + i);

        assertThat(res.getContent()).containsExactly("#1", "#2");
        assertThat(res.getPage()).isZero();
        assertThat(res.getSize()).isEqualTo(2);
        assertThat(res.getTotalElements()).isEqualTo(5);
        assertThat(res.getTotalPages()).isEqualTo(3);
        assertThat(res.isLast()).isFalse();
    }
}
