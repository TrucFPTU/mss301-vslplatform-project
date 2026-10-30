package com.vsl.learning.service;

import com.vsl.common.response.PageResponse;
import com.vsl.learning.dto.response.AttemptResponse;
import com.vsl.learning.entity.Vocabulary;

import java.util.List;

public interface AttemptService {

    PageResponse<AttemptResponse> getMyAttempts(Long userId, int page, int size);

    List<AttemptResponse> getRecentAttempts(Long userId, int limit);

    /**
     * Ghi kết quả 1 lần luyện tập (do ai-service chấm): lưu AttemptHistory,
     * cập nhật UserProgress rồi kiểm tra mở khoá thành tựu — trong cùng 1 transaction.
     */
    void recordAttempt(Long userId, Vocabulary vocabulary, boolean passed, int predictedId, double confidence);
}
