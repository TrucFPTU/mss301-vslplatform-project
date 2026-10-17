package com.vsl.learning.service;

import com.vsl.learning.dto.response.AchievementResponse;

import java.util.List;

public interface AchievementService {

    List<AchievementResponse> getAll(Long userId);

    /** Kiểm tra điều kiện và mở khoá các thành tựu mới đạt được. Gọi sau mỗi lần luyện tập. */
    void checkAndUnlock(Long userId);

    /**
     * % Chương trình học = tỉ lệ từ vựng đã LEARNED trên tổng số từ vựng.
     * Chỉ từ đã làm ĐÚNG ít nhất 1 lần mới được tính.
     */
    static int computeProficiency(long learned, long totalVocabs) {
        return totalVocabs > 0 ? (int) Math.round((double) learned / totalVocabs * 100) : 0;
    }
}
