package com.vsl.learning.service;

import com.vsl.learning.dto.response.VocabularyResponse;

import java.util.List;

public interface FavoriteService {

    /** Danh sách từ yêu thích của user, mới đánh dấu trước. */
    List<VocabularyResponse> getMine(Long userId);

    /** Đánh dấu yêu thích (idempotent: đã có thì thôi). 404 nếu từ vựng không tồn tại. */
    void add(Long userId, Long vocabularyId);

    /** Bỏ yêu thích (idempotent: chưa có thì thôi). */
    void remove(Long userId, Long vocabularyId);
}
