package com.vsl.learning.repository;

import com.vsl.learning.entity.Achievement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AchievementRepository extends JpaRepository<Achievement, Long> {
    Optional<Achievement> findByKey(String key);
    boolean existsByKey(String key);
}
