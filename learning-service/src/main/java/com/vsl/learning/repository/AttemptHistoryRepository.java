package com.vsl.learning.repository;

import com.vsl.learning.entity.AttemptHistory;
import com.vsl.learning.repository.projection.AttemptTrendProjection;
import com.vsl.learning.repository.projection.VocabularyPracticeProjection;
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

    long countByIsCorrect(boolean isCorrect);

    @Query("SELECT COUNT(DISTINCT h.userId) FROM AttemptHistory h")
    long countDistinctLearners();

    @Query("SELECT COALESCE(AVG(h.confidence), 0.0) FROM AttemptHistory h WHERE h.confidence IS NOT NULL")
    Double getAverageConfidence();

    /** Những user có ít nhất 1 lần luyện tập trong [from, to) - dùng chọn ứng viên nhắc streak. */
    @Query("SELECT DISTINCT h.userId FROM AttemptHistory h WHERE h.attemptedAt >= :from AND h.attemptedAt < :to")
    List<Long> findUserIdsPracticedBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /** Thoi diem cac lan luyen tap cua user - dung de tinh chuoi ngay hoc (streak). */
    @Query("SELECT h.attemptedAt FROM AttemptHistory h WHERE h.userId = :userId AND h.attemptedAt IS NOT NULL")
    List<LocalDateTime> findAttemptTimesByUserId(@Param("userId") Long userId);

    /** Lay danh sach luot luyen tap trong khoang thoi gian de tinh bieu do xu huong. */
    @Query("""
            SELECT h.attemptedAt as attemptedAt, h.isCorrect as isCorrect, h.userId as userId
            FROM AttemptHistory h
            WHERE h.attemptedAt >= :from AND h.attemptedAt <= :to
            ORDER BY h.attemptedAt ASC
            """)
    List<AttemptTrendProjection> findAttemptsBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /** Top tu vung duoc luyen tap nhieu nhat. */
    @Query("""
            SELECT v.id as vocabularyId, v.word as word, c.name as categoryName,
                   COUNT(h.id) as attemptCount,
                   SUM(CASE WHEN h.isCorrect = true THEN 1L ELSE 0L END) as correctCount
            FROM AttemptHistory h
            JOIN h.vocabulary v
            LEFT JOIN v.category c
            GROUP BY v.id, v.word, c.name
            ORDER BY COUNT(h.id) DESC
            """)
    List<VocabularyPracticeProjection> findTopPracticedVocabularies(Pageable pageable);

    /** Top tu vung co ty le sai cao nhat (tu kho), loc tu vung co so luot thu toi thieu minAttempts. */
    @Query("""
            SELECT v.id as vocabularyId, v.word as word, c.name as categoryName,
                   COUNT(h.id) as attemptCount,
                   SUM(CASE WHEN h.isCorrect = true THEN 1L ELSE 0L END) as correctCount
            FROM AttemptHistory h
            JOIN h.vocabulary v
            LEFT JOIN v.category c
            GROUP BY v.id, v.word, c.name
            HAVING COUNT(h.id) >= :minAttempts
            ORDER BY (CAST(SUM(CASE WHEN h.isCorrect = false THEN 1.0 ELSE 0.0 END) AS double) / COUNT(h.id)) DESC
            """)
    List<VocabularyPracticeProjection> findMostDifficultVocabularies(@Param("minAttempts") long minAttempts, Pageable pageable);
}
