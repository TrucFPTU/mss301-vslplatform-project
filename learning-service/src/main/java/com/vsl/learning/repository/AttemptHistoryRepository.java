package com.vsl.learning.repository;

import com.vsl.learning.entity.AttemptHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AttemptHistoryRepository extends JpaRepository<AttemptHistory, Long> {

    boolean existsByVocabulary_Id(Long vocabularyId);

    @Query(
            value = """
                    SELECT h FROM AttemptHistory h
                    LEFT JOIN FETCH h.vocabulary v
                    LEFT JOIN FETCH v.category
                    WHERE h.userId = :userId
                    ORDER BY h.attemptedAt DESC, h.id DESC
                    """,
            countQuery = "SELECT COUNT(h) FROM AttemptHistory h WHERE h.userId = :userId"
    )
    Page<AttemptHistory> findPageByUserId(@Param("userId") Long userId, Pageable pageable);

    long countByUserId(Long userId);

    long countByUserIdAndIsCorrect(Long userId, boolean isCorrect);

    /** Thoi diem cac lan luyen tap cua user - dung de tinh chuoi ngay hoc (streak). */
    @Query("SELECT h.attemptedAt FROM AttemptHistory h WHERE h.userId = :userId AND h.attemptedAt IS NOT NULL")
    List<LocalDateTime> findAttemptTimesByUserId(@Param("userId") Long userId);
}
