package com.vsl.learning.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class PracticeStatsResponse {
    private final long totalAttempts;
    private final long correctAttempts;
    private final long learnedCount;
    private final long totalVocabs;
    private final double accuracyRate;          // 0.0 – 100.0
    private final int proficiency;              // 0 – 100: % từ đã LEARNED
    private final int currentStreak;            // số ngày liên tiếp có luyện tập tính đến hôm nay
    private final int longestStreak;            // chuỗi ngày dài nhất
    private final List<Boolean> weekActivity;   // 7 phần tử, cũ -> mới; phần tử cuối = hôm nay
}
