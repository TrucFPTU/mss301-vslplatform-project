package com.vsl.learning.repository.projection;

import java.time.LocalDateTime;

/** Thống kê luyện tập của MỘT người học trên từng từ vựng (từ bảng attempt_history). */
public interface UserVocabularyAttemptProjection {
    Long getVocabularyId();
    Long getAttemptCount();
    Long getCorrectCount();
    LocalDateTime getLastAttemptAt();
    /** null nếu chưa ký đúng lần nào. */
    LocalDateTime getLastCorrectAt();
}
