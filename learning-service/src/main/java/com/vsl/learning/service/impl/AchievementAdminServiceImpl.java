package com.vsl.learning.service.impl;

import com.vsl.common.exception.AppException;
import com.vsl.common.exception.ErrorCode;
import com.vsl.learning.dto.request.AchievementRequest;
import com.vsl.learning.dto.response.AdminAchievementResponse;
import com.vsl.learning.entity.Achievement;
import com.vsl.learning.entity.UserAchievement;
import com.vsl.learning.repository.AchievementRepository;
import com.vsl.learning.repository.UserAchievementRepository;
import com.vsl.learning.repository.projection.AchievementUnlockCountProjection;
import com.vsl.learning.service.AchievementAdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AchievementAdminServiceImpl implements AchievementAdminService {

    private final AchievementRepository achievementRepository;
    private final UserAchievementRepository userAchievementRepository;

    @Override
    @Transactional(readOnly = true)
    public List<AdminAchievementResponse> getAllAchievements() {
        Map<Long, Long> unlockCounts = userAchievementRepository.countUnlocksGroupByAchievement().stream()
                .collect(Collectors.toMap(
                        AchievementUnlockCountProjection::getAchievementId,
                        AchievementUnlockCountProjection::getUnlockCount,
                        (a, b) -> a
                ));

        return achievementRepository.findAll().stream()
                .sorted(Comparator.comparingLong(Achievement::getId))
                .map(a -> AdminAchievementResponse.from(a, unlockCounts.getOrDefault(a.getId(), 0L)))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AdminAchievementResponse getAchievementById(Long id) {
        Achievement achievement = getAchievementOrThrow(id);
        long unlockCount = userAchievementRepository.countByAchievementId(id);
        return AdminAchievementResponse.from(achievement, unlockCount);
    }

    @Override
    @Transactional
    public AdminAchievementResponse createAchievement(AchievementRequest request) {
        String key = request.getKey().trim();
        if (achievementRepository.existsByKey(key)) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Mã thành tựu (key) đã tồn tại: " + key);
        }

        Long nextId = achievementRepository.findMaxId() + 1;
        Achievement achievement = Achievement.builder()
                .id(nextId)
                .key(key)
                .name(request.getName().trim())
                .description(request.getDescription().trim())
                .iconKey(request.getIconKey() != null ? request.getIconKey().trim() : null)
                .build();

        Achievement saved = achievementRepository.save(achievement);
        log.info("Admin created new achievement: id={} key={}", saved.getId(), saved.getKey());
        return AdminAchievementResponse.from(saved, 0L);
    }

    @Override
    @Transactional
    public AdminAchievementResponse updateAchievement(Long id, AchievementRequest request) {
        Achievement achievement = getAchievementOrThrow(id);

        String newKey = request.getKey().trim();
        if (!achievement.getKey().equalsIgnoreCase(newKey) && achievementRepository.existsByKey(newKey)) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Mã thành tựu (key) đã tồn tại: " + newKey);
        }

        achievement.setKey(newKey);
        achievement.setName(request.getName().trim());
        achievement.setDescription(request.getDescription().trim());
        achievement.setIconKey(request.getIconKey() != null ? request.getIconKey().trim() : null);

        Achievement updated = achievementRepository.save(achievement);
        long unlockCount = userAchievementRepository.countByAchievementId(id);
        log.info("Admin updated achievement: id={} key={}", updated.getId(), updated.getKey());
        return AdminAchievementResponse.from(updated, unlockCount);
    }

    @Override
    @Transactional
    public void deleteAchievement(Long id) {
        Achievement achievement = getAchievementOrThrow(id);

        // Xoá các user_achievements liên kết trước
        userAchievementRepository.deleteByAchievementId(id);
        achievementRepository.delete(achievement);
        log.info("Admin deleted achievement: id={} key={}", id, achievement.getKey());
    }

    @Override
    @Transactional
    public void grantAchievementToUser(Long achievementId, Long userId) {
        Achievement achievement = getAchievementOrThrow(achievementId);

        if (userAchievementRepository.existsByUserIdAndAchievementId(userId, achievementId)) {
            log.info("User {} already has achievement {}", userId, achievement.getKey());
            return;
        }

        UserAchievement userAchievement = UserAchievement.builder()
                .userId(userId)
                .achievement(achievement)
                .unlockedAt(LocalDateTime.now())
                .build();

        userAchievementRepository.save(userAchievement);
        log.info("Admin manually granted achievement {} to userId={}", achievement.getKey(), userId);
    }

    private Achievement getAchievementOrThrow(Long id) {
        return achievementRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Không tìm thấy thành tựu với id: " + id));
    }
}
