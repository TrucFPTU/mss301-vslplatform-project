package com.vsl.learning.repository;

import com.vsl.learning.entity.LearningStatus;
import com.vsl.learning.entity.UserProgress;
import com.vsl.learning.repository.projection.CategoryLearnedCountProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserProgressRepository extends JpaRepository<UserProgress, Long> {

    Optional<UserProgress> findByUserIdAndVocabulary_Id(Long userId, Long vocabularyId);

    boolean existsByVocabulary_Id(Long vocabularyId);

    @Query("SELECT p FROM UserProgress p " +
           "JOIN FETCH p.vocabulary v " +
           "LEFT JOIN FETCH v.category " +
           "WHERE p.userId = :userId " +
           "ORDER BY p.lastAttemptedAt DESC")
    List<UserProgress> findAllWithVocabularyByUserId(@Param("userId") Long userId);

    long countByUserIdAndLearningStatus(Long userId, LearningStatus status);

    long countByLearningStatus(LearningStatus status);

    /** So luot tu vung dat trang thai LEARNED theo tung categoryId. */
    @Query("""
            SELECT v.category.id as categoryId, COUNT(p.id) as learnedCount
            FROM UserProgress p
            JOIN p.vocabulary v
            WHERE p.learningStatus = com.vsl.learning.entity.LearningStatus.LEARNED
              AND v.category.id IS NOT NULL
            GROUP BY v.category.id
            """)
    List<CategoryLearnedCountProjection> countLearnedPerCategory();
}

