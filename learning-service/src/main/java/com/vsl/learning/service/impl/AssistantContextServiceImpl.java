package com.vsl.learning.service.impl;

import com.vsl.learning.dto.response.AssistantContextResponse;
import com.vsl.learning.dto.response.AssistantContextResponse.CategoryItem;
import com.vsl.learning.dto.response.AssistantContextResponse.Recommendation;
import com.vsl.learning.dto.response.AssistantContextResponse.VocabularyItem;
import com.vsl.learning.entity.Category;
import com.vsl.learning.entity.LearningStatus;
import com.vsl.learning.entity.UserProgress;
import com.vsl.learning.entity.Vocabulary;
import com.vsl.learning.repository.CategoryRepository;
import com.vsl.learning.repository.UserProgressRepository;
import com.vsl.learning.repository.VocabularyRepository;
import com.vsl.learning.service.AssistantContextService;
import com.vsl.learning.service.PracticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
    /** Tối đa số gợi ý; trong đó tối đa MAX_REVIEW từ cần ôn, còn lại là từ mới. */
    static final int MAX_RECOMMENDATIONS = 5;
    static final int MAX_REVIEW = 3;

    private final CategoryRepository categoryRepository;
    private final VocabularyRepository vocabularyRepository;
    private final UserProgressRepository userProgressRepository;
    private final PracticeService practiceService;

    @Override
    @Transactional(readOnly = true)
    public AssistantContextResponse getContext(Long userId) {
        List<Category> categories = categoryRepository.findAll(Sort.by("id"));
        List<Vocabulary> vocabularies = vocabularyRepository.findAllByOrderByIdAsc();
        Map<Long, UserProgress> progress = userProgressRepository.findAllWithVocabularyByUserId(userId).stream()
                .collect(Collectors.toMap(p -> p.getVocabulary().getId(), Function.identity(), (a, b) -> a));

        List<VocabularyItem> items = vocabularies.stream().map(v -> toItem(v, progress.get(v.getId()))).toList();
        return AssistantContextResponse.builder()
                .totalCategories(categories.size())
                .totalVocabularies(vocabularies.size())
                .practicableVocabularies(items.stream().filter(VocabularyItem::practicable).count())
                .stats(practiceService.getStats(userId))
                .categories(summarize(categories, items))
                .vocabularies(items)
                .recommendations(recommend(categories, items, progress))
                .build();
    }

    /**
     * Gợi ý theo quy tắc cố định (dễ giải thích, không phụ thuộc AI):
     * 1. Từ đã luyện nhưng chưa ký đúng (LEARNING), lâu chưa tập nhất trước - tối đa {@value MAX_REVIEW} từ.
     * 2. Từ mới luyện được: ưu tiên chủ đề đang học dở (đã đụng nhiều từ nhất), rồi các chủ đề còn lại theo thứ tự.
     */
    static List<Recommendation> recommend(List<Category> categories, List<VocabularyItem> items,
                                          Map<Long, UserProgress> progress) {
        List<Recommendation> result = new ArrayList<>();
        items.stream()
                .filter(v -> v.practicable() && LearningStatus.LEARNING.name().equals(v.status()))
                .sorted(Comparator.comparing((VocabularyItem v) -> lastAttempt(progress.get(v.id())),
                        Comparator.nullsFirst(Comparator.naturalOrder())))
                .limit(MAX_REVIEW)
                .forEach(v -> result.add(new Recommendation(v.id(), v.word(),
                        "Bạn đã luyện nhưng chưa ký đúng — nên ôn lại")));

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

    private static List<CategoryItem> summarize(List<Category> categories, List<VocabularyItem> items) {
        return categories.stream().map(c -> {
            List<VocabularyItem> list = items.stream().filter(v -> c.getId().equals(v.categoryId())).toList();
            return new CategoryItem(c.getId(), c.getName(), list.size(),
                    count(list, LearningStatus.LEARNED.name()), count(list, LearningStatus.LEARNING.name()));
        }).toList();
    }

    private static VocabularyItem toItem(Vocabulary v, UserProgress p) {
        Category c = v.getCategory();
        boolean practicable = v.getExpectedId() != null && v.getVideoTutorialUrl() != null;
        String status = p == null || p.getLearningStatus() == null ? NEW : p.getLearningStatus().name();
        return new VocabularyItem(v.getId(), v.getWord(), c == null ? null : c.getId(), c == null ? null : c.getName(),
                v.getImageUrl(), practicable, status);
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
}
