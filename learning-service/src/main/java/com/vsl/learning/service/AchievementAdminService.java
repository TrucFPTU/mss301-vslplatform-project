package com.vsl.learning.service;

import com.vsl.learning.dto.request.AchievementRequest;
import com.vsl.learning.dto.response.AdminAchievementResponse;

import java.util.List;

public interface AchievementAdminService {

    List<AdminAchievementResponse> getAllAchievements();

    AdminAchievementResponse getAchievementById(Long id);

    AdminAchievementResponse createAchievement(AchievementRequest request);

    AdminAchievementResponse updateAchievement(Long id, AchievementRequest request);

    void deleteAchievement(Long id);

    void grantAchievementToUser(Long achievementId, Long userId);
}
