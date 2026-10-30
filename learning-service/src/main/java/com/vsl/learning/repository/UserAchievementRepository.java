package com.vsl.learning.repository;

import com.vsl.learning.entity.UserAchievement;
import com.vsl.learning.repository.projection.AchievementUnlockCountProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserAchievementRepository extends JpaRepository<UserAchievement, Long> {

    @Query("SELECT ua FROM UserAchievement ua JOIN FETCH ua.achievement WHERE ua.userId = :userId")
    List<UserAchievement> findAllWithAchievementByUserId(@Param("userId") Long userId);

    @Query("SELECT ua.achievement.id AS achievementId, COUNT(ua) AS unlockCount FROM UserAchievement ua GROUP BY ua.achievement.id")
    List<AchievementUnlockCountProjection> countUnlocksGroupByAchievement();

    long countByAchievementId(Long achievementId);

    boolean existsByUserIdAndAchievementId(Long userId, Long achievementId);

    @Modifying
    @Query("DELETE FROM UserAchievement ua WHERE ua.achievement.id = :achievementId")
    void deleteByAchievementId(@Param("achievementId") Long achievementId);
}
