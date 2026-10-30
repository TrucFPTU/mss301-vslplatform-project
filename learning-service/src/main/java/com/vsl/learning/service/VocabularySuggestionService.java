package com.vsl.learning.service;

import com.vsl.common.response.PageResponse;
import com.vsl.learning.dto.request.CreateVocabularySuggestionRequest;
import com.vsl.learning.dto.response.VocabularySuggestionResponse;

import java.util.List;

public interface VocabularySuggestionService {

    /** Người dùng gửi đề xuất từ vựng mới. Chặn nếu từ đã tồn tại (bỏ dấu, không hoa/thường), toàn hệ thống. */
    VocabularySuggestionResponse submit(Long userId, CreateVocabularySuggestionRequest request);

    /** Danh sách đề xuất của chính người dùng. */
    List<VocabularySuggestionResponse> getMine(Long userId);

    /** Người dùng sửa đề xuất của chính mình khi còn PENDING. */
    VocabularySuggestionResponse update(Long userId, Long id, CreateVocabularySuggestionRequest request);

    /** Người dùng thu hồi (xóa) đề xuất của chính mình khi còn PENDING. */
    void withdraw(Long userId, Long id);

    /** Admin: danh sách tất cả đề xuất (mới nhất trước). */
    PageResponse<VocabularySuggestionResponse> getAll(int page, int size);

    /** Admin: số đề xuất đang chờ (PENDING). */
    long countPending();

    /** Admin: đánh dấu đã xem. */
    void markReviewed(Long id);
}
