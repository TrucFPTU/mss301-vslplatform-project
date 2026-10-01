package com.vsl.learning.repository;

import com.vsl.learning.entity.Vocabulary;
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

    /** Cac class AI hop le = expectedId cua nhung tu vung da co video mau. */
    @Query("SELECT DISTINCT v.expectedId FROM Vocabulary v " +
           "WHERE v.expectedId IS NOT NULL AND v.videoTutorialUrl IS NOT NULL")
    List<Integer> findExpectedIdsWithVideo();
}
