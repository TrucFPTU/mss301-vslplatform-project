package com.vsl.learning.service;

import com.vsl.learning.dto.response.AchievementResponse;
import com.vsl.learning.entity.Achievement;
import com.vsl.learning.entity.LearningStatus;
import com.vsl.learning.entity.UserAchievement;
import com.vsl.learning.repository.AchievementRepository;
import com.vsl.learning.repository.AttemptHistoryRepository;
import com.vsl.learning.repository.UserAchievementRepository;
import com.vsl.learning.repository.UserProgressRepository;
import com.vsl.learning.repository.VocabularyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AchievementService {

    private final AchievementRepository achievementRepository;
    private final UserAchievementRepository userAchievementRepository;
    private final AttemptHistoryRepository attemptHistoryRepository;
    private final UserProgressRepository userProgressRepository;
    private final VocabularyRepository vocabularyRepository;

    record AchievementDef(Long id, String key, String name, String description, String iconKey) {}

    private static final List<AchievementDef> DEFINITIONS = List.of(
            new AchievementDef(1L, "FIRST_STEP",      "Bước Đầu",   "Hoàn thành lần thực hành đầu tiên",       "directions_walk"),
            new AchievementDef(2L, "CORRECT_10",      "Chính Xác",  "Đạt 10 lần đánh giá đúng",                "check_circle"),
            new AchievementDef(3L, "CORRECT_50",      "Thành Thạo", "Đạt 50 lần đánh giá đúng",                "military_tech"),
            new AchievementDef(4L, "LEARNED_5",       "Nhập Môn",   "Học thuộc 5 từ vựng",                     "school"),
            new AchievementDef(5L, "LEARNED_20",      "Tiến Bộ",    "Học thuộc 20 từ vựng",                    "trending_up"),
            new AchievementDef(6L, "ALL_LEARNED",     "Đại Sư",     "Học thuộc tất cả từ vựng trong hệ thống", "emoji_events"),
            new AchievementDef(7L, "PROFICIENCY_50",  "Nửa Đường",  "Đạt độ thành thạo 50%",                   "star_half"),
            new AchievementDef(8L, "PROFICIENCY_100", "Hoàn Hảo",   "Đạt độ thành thạo 100%",                  "stars")
    );

    /** Seed danh mục thành tựu khi khởi động (idempotent). */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seedAchievements() {
        for (AchievementDef def : DEFINITIONS) {
            if (!achievementRepository.existsByKey(def.key())) {
                achievementRepository.save(Achievement.builder()
                        .id(def.id())
                        .key(def.key())
                        .name(def.name())
                        .description(def.description())
                        .iconKey(def.iconKey())
                        .build());
            }
        }
        log.info("Achievements seeded ({} definitions).", DEFINITIONS.size());
    }

    @Transactional(readOnly = true)
    public List<AchievementResponse> getAll(Long userId) {
        Map<String, LocalDateTime> unlockedAt = unlockedAtByKey(userId);

        return achievementRepository.findAll().stream()
                .sorted(Comparator.comparingLong(Achievement::getId))
                .map(a -> AchievementResponse.builder()
                        .id(a.getId())
                        .key(a.getKey())
                        .name(a.getName())
                        .description(a.getDescription())
                        .iconKey(a.getIconKey())
                        .unlocked(unlockedAt.containsKey(a.getKey()))
                        .unlockedAt(unlockedAt.get(a.getKey()))
                        .build())
                .toList();
    }

    /** Kiểm tra điều kiện và mở khoá các thành tựu mới đạt được. Gọi sau mỗi lần luyện tập. */
    @Transactional
    public void checkAndUnlock(Long userId) {
        long totalAttempts = attemptHistoryRepository.countByUserId(userId);
        long correctAttempts = attemptHistoryRepository.countByUserIdAndIsCorrect(userId, true);
        long learnedCount = userProgressRepository.countByUserIdAndLearningStatus(userId, LearningStatus.LEARNED);
        long totalVocabs = vocabularyRepository.count();
        int proficiency = computeProficiency(learnedCount, totalVocabs);

        Map<String, Boolean> conditions = new LinkedHashMap<>();
        conditions.put("FIRST_STEP", totalAttempts >= 1);
        conditions.put("CORRECT_10", correctAttempts >= 10);
        conditions.put("CORRECT_50", correctAttempts >= 50);
        conditions.put("LEARNED_5", learnedCount >= 5);
        conditions.put("LEARNED_20", learnedCount >= 20);
        conditions.put("ALL_LEARNED", totalVocabs > 0 && learnedCount >= totalVocabs);
        conditions.put("PROFICIENCY_50", proficiency >= 50);
        conditions.put("PROFICIENCY_100", proficiency >= 100);

        Set<String> alreadyUnlocked = unlockedAtByKey(userId).keySet();
        LocalDateTime now = LocalDateTime.now();
        conditions.forEach((key, met) -> {
            if (met && !alreadyUnlocked.contains(key)) {
                achievementRepository.findByKey(key).ifPresent(achievement -> {
                    userAchievementRepository.save(UserAchievement.builder()
                            .userId(userId)
                            .achievement(achievement)
                            .unlockedAt(now)
                            .build());
                    log.info("Achievement unlocked: userId={} key={}", userId, key);
                });
            }
        });
    }

    /**
     * % Chương trình học = tỉ lệ từ vựng đã LEARNED trên tổng số từ vựng.
     * Chỉ từ đã làm ĐÚNG ít nhất 1 lần mới được tính.
     */
    public static int computeProficiency(long learned, long totalVocabs) {
        return totalVocabs > 0 ? (int) Math.round((double) learned / totalVocabs * 100) : 0;
    }

    private Map<String, LocalDateTime> unlockedAtByKey(Long userId) {
        return userAchievementRepository.findAllWithAchievementByUserId(userId).stream()
                .collect(Collectors.toMap(ua -> ua.getAchievement().getKey(), UserAchievement::getUnlockedAt,
                        (a, b) -> a));
    }
}
