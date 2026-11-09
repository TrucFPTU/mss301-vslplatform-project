package com.vsl.learning.service.impl;

import com.vsl.learning.dto.response.AssistantContextResponse;
import com.vsl.learning.dto.response.AssistantContextResponse.CategoryItem;
import com.vsl.learning.dto.response.AssistantContextResponse.Recommendation;
import com.vsl.learning.dto.response.AssistantContextResponse.VocabularyItem;
import com.vsl.learning.entity.Category;
import com.vsl.learning.entity.LearningStatus;
import com.vsl.learning.entity.UserProgress;
import com.vsl.learning.entity.Vocabulary;
import com.vsl.learning.repository.AttemptHistoryRepository;
import com.vsl.learning.repository.CategoryRepository;
import com.vsl.learning.repository.UserProgressRepository;
import com.vsl.learning.repository.VocabularyRepository;
import com.vsl.learning.repository.projection.UserVocabularyAttemptProjection;
import com.vsl.learning.service.AssistantContextService;
import com.vsl.learning.service.PracticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AssistantContextServiceImpl implements AssistantContextService {

    static final String NEW = "NEW";
    /**
     * Tối đa số gợi ý: tối đa MAX_REVIEW từ đang học dở (chưa ký đúng), tối đa MAX_SPACED_REVIEW từ đã thuộc
     * tới hạn ôn lại, còn lại là từ mới.
     */
    public static final int MAX_RECOMMENDATIONS = 6;
    static final int MAX_REVIEW = 3;
    static final int MAX_SPACED_REVIEW = 2;
    /**
     * Ôn giãn cách (kiểu hộp Leitner): ký đúng càng nhiều lần thì càng lâu mới cần ôn lại.
     * Phần tử thứ i = số ngày chờ sau lần đúng gần nhất khi đã ký đúng (i+1) lần; từ lần thứ 5 trở đi dùng phần tử cuối.
     */
    static final int[] REVIEW_INTERVAL_DAYS = {1, 3, 7, 14, 30};

    private final CategoryRepository categoryRepository;
    private final VocabularyRepository vocabularyRepository;
    private final UserProgressRepository userProgressRepository;
    private final AttemptHistoryRepository attemptHistoryRepository;
    private final PracticeService practiceService;

    @Override
    @Transactional(readOnly = true)
    public AssistantContextResponse getContext(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        List<Category> categories = categoryRepository.findAll(Sort.by("id"));
        List<Vocabulary> vocabularies = vocabularyRepository.findAllByOrderByIdAsc();
        Map<Long, UserProgress> progress = userProgressRepository.findAllWithVocabularyByUserId(userId).stream()
                .collect(Collectors.toMap(p -> p.getVocabulary().getId(), Function.identity(), (a, b) -> a));
        Map<Long, UserVocabularyAttemptProjection> attempts = attemptHistoryRepository
                .summarizeByVocabularyForUser(userId).stream()
                .collect(Collectors.toMap(UserVocabularyAttemptProjection::getVocabularyId, Function.identity(),
                        (a, b) -> a));

        List<VocabularyItem> items = vocabularies.stream()
                .map(v -> toItem(v, progress.get(v.getId()), attempts.get(v.getId()), now)).toList();
        return AssistantContextResponse.builder()
                .totalCategories(categories.size())
                .totalVocabularies(vocabularies.size())
                .practicableVocabularies(items.stream().filter(VocabularyItem::practicable).count())
                .stats(practiceService.getStats(userId))
                .categories(summarize(categories, items))
                .vocabularies(items)
                .recommendations(recommend(categories, items, progress, attempts, now))
                .build();
    }

    /**
     * Gợi ý theo quy tắc cố định (dễ giải thích, không phụ thuộc AI):
     * 1. Từ đã luyện nhưng chưa ký đúng (LEARNING), lâu chưa tập nhất trước - tối đa {@value MAX_REVIEW} từ.
     * 2. Từ đã thuộc nhưng tới hạn ôn lại theo {@link #REVIEW_INTERVAL_DAYS}, quá hạn nhiều nhất trước
     *    - tối đa {@value MAX_SPACED_REVIEW} từ.
     * 3. Từ mới luyện được: ưu tiên chủ đề đang học dở (đã đụng nhiều từ nhất), rồi các chủ đề còn lại theo thứ tự.
     */
    static List<Recommendation> recommend(List<Category> categories, List<VocabularyItem> items,
                                          Map<Long, UserProgress> progress,
                                          Map<Long, UserVocabularyAttemptProjection> attempts, LocalDateTime now) {
        List<Recommendation> result = new ArrayList<>();
        items.stream()
                .filter(v -> v.practicable() && LearningStatus.LEARNING.name().equals(v.status()))
                .sorted(Comparator.comparing((VocabularyItem v) -> lastAttempt(progress.get(v.id())),
                        Comparator.nullsFirst(Comparator.naturalOrder())))
                .limit(MAX_REVIEW)
                .forEach(v -> result.add(new Recommendation(v.id(), v.word(),
                        "Bạn đã luyện nhưng chưa ký đúng — nên ôn lại")));

        record Due(VocabularyItem item, long days, double overdue) {
        }
        items.stream()
                .filter(v -> v.practicable() && LearningStatus.LEARNED.name().equals(v.status()))
                .map(v -> {
                    LocalDateTime ref = lastCorrect(attempts.get(v.id()), progress.get(v.id()));
                    if (ref == null) return null;
                    long days = ChronoUnit.DAYS.between(ref.toLocalDate(), now.toLocalDate());
                    int interval = reviewIntervalDays(v.correctCount());
                    return days >= interval ? new Due(v, days, (double) days / interval) : null;
                })
                .filter(d -> d != null)
                .sorted(Comparator.comparingDouble(Due::overdue).reversed())
                .limit(MAX_SPACED_REVIEW)
                .forEach(d -> result.add(new Recommendation(d.item().id(), d.item().word(),
                        "Đã thuộc nhưng " + d.days() + " ngày chưa ôn — ôn lại để không quên")));

        Map<Long, List<VocabularyItem>> byCategory = items.stream()
                .filter(v -> v.categoryId() != null)
                .collect(Collectors.groupingBy(VocabularyItem::categoryId, LinkedHashMap::new, Collectors.toList()));
        List<Category> order = categories.stream()
                .sorted(Comparator.comparingLong((Category c) -> -touched(byCategory.get(c.getId())))
                        .thenComparing(Category::getId))
                .toList();
        for (Category c : order) {
            List<VocabularyItem> list = byCategory.getOrDefault(c.getId(), List.of());
            boolean started = touched(list) > 0;
            for (VocabularyItem v : list) {
                if (result.size() >= MAX_RECOMMENDATIONS) return result;
                if (v.practicable() && NEW.equals(v.status())) {
                    result.add(new Recommendation(v.id(), v.word(), started
                            ? "Từ tiếp theo trong chủ đề “" + c.getName() + "” bạn đang học"
                            : "Mở đầu chủ đề “" + c.getName() + "”"));
                }
            }
        }
        return result;
    }

    /** Số ngày chờ ôn lại sau lần đúng gần nhất, theo số lần đã ký đúng (ít nhất coi như 1 lần). */
    public static int reviewIntervalDays(long correctCount) {
        int index = (int) Math.min(Math.max(correctCount, 1), REVIEW_INTERVAL_DAYS.length) - 1;
        return REVIEW_INTERVAL_DAYS[index];
    }

    private static List<CategoryItem> summarize(List<Category> categories, List<VocabularyItem> items) {
        return categories.stream().map(c -> {
            List<VocabularyItem> list = items.stream().filter(v -> c.getId().equals(v.categoryId())).toList();
            return new CategoryItem(c.getId(), c.getName(), list.size(),
                    count(list, LearningStatus.LEARNED.name()), count(list, LearningStatus.LEARNING.name()));
        }).toList();
    }

    private static VocabularyItem toItem(Vocabulary v, UserProgress p, UserVocabularyAttemptProjection a,
                                         LocalDateTime now) {
        Category c = v.getCategory();
        boolean practicable = v.getExpectedId() != null && v.getVideoTutorialUrl() != null;
        String status = p == null || p.getLearningStatus() == null ? NEW : p.getLearningStatus().name();
        long attemptCount = a == null || a.getAttemptCount() == null ? 0 : a.getAttemptCount();
        long correctCount = a == null || a.getCorrectCount() == null ? 0 : a.getCorrectCount();
        LocalDateTime last = a != null && a.getLastAttemptAt() != null ? a.getLastAttemptAt() : lastAttempt(p);
        Integer daysSince = last == null ? null
                : (int) Math.max(0, ChronoUnit.DAYS.between(last.toLocalDate(), now.toLocalDate()));
        return new VocabularyItem(v.getId(), v.getWord(), c == null ? null : c.getId(), c == null ? null : c.getName(),
                v.getImageUrl(), practicable, status, attemptCount, correctCount, daysSince);
    }

    private static long touched(List<VocabularyItem> list) {
        return list == null ? 0 : list.stream().filter(v -> !NEW.equals(v.status())).count();
    }

    private static int count(List<VocabularyItem> list, String status) {
        return (int) list.stream().filter(v -> status.equals(v.status())).count();
    }

    private static LocalDateTime lastAttempt(UserProgress p) {
        return p == null ? null : p.getLastAttemptedAt();
    }

    /** Lần ký đúng gần nhất; dữ liệu cũ không có lịch sử thì dùng lần luyện gần nhất trong tiến độ. */
    private static LocalDateTime lastCorrect(UserVocabularyAttemptProjection a, UserProgress p) {
        if (a != null && a.getLastCorrectAt() != null) return a.getLastCorrectAt();
        return lastAttempt(p);
    }
}
