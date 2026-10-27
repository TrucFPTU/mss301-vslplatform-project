package com.vsl.learning.repository;

import com.vsl.learning.entity.SuggestionStatus;
import com.vsl.learning.entity.VocabularySuggestion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface VocabularySuggestionRepository extends JpaRepository<VocabularySuggestion, Long> {

    @Query(value = "SELECT s FROM VocabularySuggestion s JOIN FETCH s.category",
           countQuery = "SELECT COUNT(s) FROM VocabularySuggestion s")
    Page<VocabularySuggestion> findAllWithDetails(Pageable pageable);

    @Query("SELECT s FROM VocabularySuggestion s JOIN FETCH s.category " +
           "WHERE s.requesterId = :userId ORDER BY s.createdAt DESC")
    List<VocabularySuggestion> findMineWithDetails(@Param("userId") Long userId);

    long countByStatus(SuggestionStatus status);

    /**
     * Các đề xuất đang chờ duyệt - dùng để chặn đề xuất TRÙNG (so khớp bỏ dấu / hoa-thường
     * ở tầng Java qua VietnameseText.fold). Danh sách PENDING thực tế rất nhỏ.
     */
    List<VocabularySuggestion> findByStatus(SuggestionStatus status);
}
