package com.vsl.learning.repository;

import com.vsl.learning.entity.Vocabulary;
import com.vsl.learning.repository.projection.VocabularyPracticeProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface VocabularyRepository extends JpaRepository<Vocabulary, Long> {

    Optional<Vocabulary> findFirstByExpectedId(Integer expectedId);

    boolean existsByCategoryId(Long categoryId);

    @EntityGraph(attributePaths = {"category"})
    Optional<Vocabulary> findWithCategoryById(Long id);

    @EntityGraph(attributePaths = {"category"})
    Page<Vocabulary> findAllBy(Pageable pageable);

    @EntityGraph(attributePaths = {"category"})
    Page<Vocabulary> findByWordContainingIgnoreCase(String keyword, Pageable pageable);

    @EntityGraph(attributePaths = {"category"})
    Page<Vocabulary> findByCategoryId(Long categoryId, Pageable pageable);

    /** Toàn bộ từ vựng (chỉ chuỗi word) - dùng để so khớp trùng bỏ dấu khi duyệt đề xuất. */
    @Query("SELECT v.word FROM Vocabulary v")
    List<String> findAllWords();

    /** Cac class AI hop le = expectedId cua nhung tu vung da co video mau. */
    @Query("SELECT DISTINCT v.expectedId FROM Vocabulary v " +
           "WHERE v.expectedId IS NOT NULL AND v.videoTutorialUrl IS NOT NULL")
    List<Integer> findExpectedIdsWithVideo();

    /** Danh sach tu vung it duoc luyen tap nhat (bao gom ca tu chua duoc tap lan nao). */
    @Query("""
            SELECT v.id as vocabularyId, v.word as word, c.name as categoryName,
                   COUNT(h.id) as attemptCount,
                   SUM(CASE WHEN h.isCorrect = true THEN 1L ELSE 0L END) as correctCount
            FROM Vocabulary v
            LEFT JOIN v.category c
            LEFT JOIN AttemptHistory h ON h.vocabulary.id = v.id
            GROUP BY v.id, v.word, c.name
            ORDER BY COUNT(h.id) ASC, v.id ASC
            """)
    List<VocabularyPracticeProjection> findLeastPracticedVocabularies(Pageable pageable);
}
