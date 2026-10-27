package com.vsl.learning.repository;

import com.vsl.learning.entity.Category;
import com.vsl.learning.repository.projection.CategoryPracticeProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    boolean existsByName(String name);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    /** Thong ke tong so tu vung, luot luyen tap va so luot dung theo tung category. */
    @Query("""
            SELECT c.id as categoryId, c.name as categoryName,
                   COUNT(DISTINCT v.id) as totalVocabularies,
                   COUNT(h.id) as totalAttempts,
                   SUM(CASE WHEN h.isCorrect = true THEN 1L ELSE 0L END) as correctAttempts
            FROM Category c
            LEFT JOIN Vocabulary v ON v.category.id = c.id
            LEFT JOIN AttemptHistory h ON h.vocabulary.id = v.id
            GROUP BY c.id, c.name
            ORDER BY c.id ASC
            """)
    List<CategoryPracticeProjection> getCategoryPracticeStats();
}

