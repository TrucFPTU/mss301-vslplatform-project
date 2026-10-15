package com.vsl.learning.repository;

import com.vsl.learning.entity.Achievement;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface AchievementRepository extends JpaRepository<Achievement, Long> {
    Optional<Achievement> findByKey(String key);
    boolean existsByKey(String key);

    @Query("SELECT COALESCE(MAX(a.id), 0) FROM Achievement a")
    Long findMaxId();
}
