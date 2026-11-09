package com.vsl.learning.service;

import com.vsl.learning.dto.response.AssistantContextResponse;
import com.vsl.learning.dto.response.AssistantContextResponse.Recommendation;
import com.vsl.learning.dto.response.AssistantContextResponse.VocabularyItem;
import com.vsl.learning.dto.response.PracticeStatsResponse;
import com.vsl.learning.entity.Category;
import com.vsl.learning.entity.LearningStatus;
import com.vsl.learning.entity.UserProgress;
import com.vsl.learning.entity.Vocabulary;
import com.vsl.learning.repository.AttemptHistoryRepository;
import com.vsl.learning.repository.CategoryRepository;
import com.vsl.learning.repository.UserProgressRepository;
import com.vsl.learning.repository.VocabularyRepository;
import com.vsl.learning.repository.projection.UserVocabularyAttemptProjection;
import com.vsl.learning.service.impl.AssistantContextServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssistantContextServiceTest {

    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private VocabularyRepository vocabularyRepository;
    @Mock
    private UserProgressRepository userProgressRepository;
    @Mock
    private AttemptHistoryRepository attemptHistoryRepository;
    @Mock
    private PracticeService practiceService;

    @InjectMocks
    private AssistantContextServiceImpl service;

    private final Category greet = Category.builder().id(1L).name("Chào hỏi").build();
    private final Category family = Category.builder().id(2L).name("Gia đình").build();

    private static Vocabulary vocab(long id, String word, Category c, boolean practicable) {
        return Vocabulary.builder().id(id).word(word).category(c)
                .expectedId(practicable ? (int) id : null)
                .videoTutorialUrl(practicable ? "http://v/" + id : null).build();
    }

    private static UserProgress progress(Vocabulary v, LearningStatus status, LocalDateTime at) {
        UserProgress p = new UserProgress();
        p.setUserId(7L);
        p.setVocabulary(v);
        p.setLearningStatus(status);
        p.setLastAttemptedAt(at);
        return p;
    }

    record Attempts(Long getVocabularyId, Long getAttemptCount, Long getCorrectCount, LocalDateTime getLastAttemptAt,
                    LocalDateTime getLastCorrectAt) implements UserVocabularyAttemptProjection {
    }

    private void givenData(List<Vocabulary> vocabs, List<UserProgress> progress) {
        givenData(vocabs, progress, List.of());
    }

    private void givenData(List<Vocabulary> vocabs, List<UserProgress> progress,
                           List<UserVocabularyAttemptProjection> attempts) {
        when(categoryRepository.findAll(any(Sort.class))).thenReturn(List.of(greet, family));
        when(vocabularyRepository.findAllByOrderByIdAsc()).thenReturn(vocabs);
        when(userProgressRepository.findAllWithVocabularyByUserId(7L)).thenReturn(progress);
        when(attemptHistoryRepository.summarizeByVocabularyForUser(7L)).thenReturn(attempts);
        when(practiceService.getStats(7L)).thenReturn(PracticeStatsResponse.builder().build());
    }

    @Test
    @DisplayName("Người mới: gợi ý từ luyện được đầu tiên, bỏ qua từ chưa có video/class AI")
    void newLearner_StartsFirstCategory() {
        Vocabulary a = vocab(1, "xin chào", greet, false);
        Vocabulary b = vocab(2, "tạm biệt", greet, true);
        Vocabulary c = vocab(3, "bố", family, true);
        givenData(List.of(a, b, c), List.of());

        AssistantContextResponse ctx = service.getContext(7L);

        assertThat(ctx.getTotalVocabularies()).isEqualTo(3);
        assertThat(ctx.getPracticableVocabularies()).isEqualTo(2);
        assertThat(ctx.getRecommendations()).extracting(Recommendation::vocabularyId).containsExactly(2L, 3L);
        assertThat(ctx.getRecommendations().get(0).reason()).contains("Mở đầu chủ đề “Chào hỏi”");
    }

    @Test
    @DisplayName("Từ đang học (LEARNING) lên trước, rồi từ đã thuộc tới hạn ôn, rồi tới chủ đề đang học dở")
    void reviewFirst_ThenUnfinishedCategory() {
        Vocabulary g1 = vocab(1, "xin chào", greet, true);
        Vocabulary g2 = vocab(2, "tạm biệt", greet, true);
        Vocabulary f1 = vocab(3, "bố", family, true);
        Vocabulary f2 = vocab(4, "mẹ", family, true);
        Vocabulary f3 = vocab(5, "anh", family, true);
        LocalDateTime now = LocalDateTime.now();
        givenData(List.of(g1, g2, f1, f2, f3), List.of(
                progress(f1, LearningStatus.LEARNING, now),
                progress(f2, LearningStatus.LEARNING, now.minusDays(3)),
                progress(g1, LearningStatus.LEARNED, now.minusDays(1))));

        AssistantContextResponse ctx = service.getContext(7L);

        // Ôn: mẹ (3 ngày trước) rồi bố; "xin chào" thuộc 1 ngày trước -> tới hạn ôn (1 ngày);
        // từ mới: "Gia đình" đã đụng 2 từ > "Chào hỏi" 1 từ.
        assertThat(ctx.getRecommendations()).extracting(Recommendation::vocabularyId).containsExactly(4L, 3L, 1L, 5L, 2L);
        assertThat(ctx.getRecommendations().get(2).reason()).contains("Đã thuộc nhưng 1 ngày chưa ôn");
        assertThat(ctx.getRecommendations().get(3).reason()).contains("đang học");
        assertThat(ctx.getCategories().get(1).learningCount()).isEqualTo(2);
        assertThat(ctx.getCategories().get(0).learnedCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Tối đa 6 gợi ý, trong đó tối đa 3 từ đang học dở")
    void capsRecommendations() {
        List<Vocabulary> vocabs = new ArrayList<>();
        List<UserProgress> progress = new ArrayList<>();
        for (long i = 1; i <= 10; i++) {
            Vocabulary v = vocab(i, "từ " + i, greet, true);
            vocabs.add(v);
            if (i <= 4) progress.add(progress(v, LearningStatus.LEARNING, LocalDateTime.now().minusHours(i)));
        }
        givenData(vocabs, progress);

        List<Recommendation> recs = service.getContext(7L).getRecommendations();

        assertThat(recs).hasSize(AssistantContextServiceImpl.MAX_RECOMMENDATIONS);
        assertThat(recs.stream().filter(r -> r.reason().contains("ôn lại")).count()).isEqualTo(3);
    }

    @Test
    @DisplayName("Ôn giãn cách: ký đúng càng nhiều lần càng lâu mới phải ôn; quá hạn nhiều nhất trước, tối đa 2 từ")
    void spacedReview_DueByCorrectCount() {
        Vocabulary a = vocab(1, "xin chào", greet, true);
        Vocabulary b = vocab(2, "tạm biệt", greet, true);
        Vocabulary c = vocab(3, "bố", family, true);
        Vocabulary d = vocab(4, "mẹ", family, true);
        LocalDateTime now = LocalDateTime.now();
        givenData(List.of(a, b, c, d),
                List.of(progress(a, LearningStatus.LEARNED, now.minusDays(5)),
                        progress(b, LearningStatus.LEARNED, now.minusDays(2)),
                        progress(c, LearningStatus.LEARNED, now.minusDays(40)),
                        progress(d, LearningStatus.LEARNED, now.minusDays(4))),
                List.of(
                        // đúng 3 lần, lần cuối 5 ngày trước -> hạn 7 ngày -> CHƯA tới hạn
                        new Attempts(1L, 4L, 3L, now.minusDays(5), now.minusDays(5)),
                        // đúng 1 lần, 2 ngày trước -> hạn 1 ngày -> quá hạn gấp 2
                        new Attempts(2L, 3L, 1L, now.minusDays(2), now.minusDays(2)),
                        // đúng 6 lần, 40 ngày trước -> hạn 30 ngày -> quá hạn gấp 1,33
                        new Attempts(3L, 6L, 6L, now.minusDays(40), now.minusDays(40)),
                        // đúng 2 lần, 4 ngày trước -> hạn 3 ngày -> quá hạn gấp 1,33 nhưng bị cắt (tối đa 2)
                        new Attempts(4L, 2L, 2L, now.minusDays(4), now.minusDays(4))));

        List<Recommendation> recs = service.getContext(7L).getRecommendations();

        assertThat(recs).extracting(Recommendation::vocabularyId).hasSize(2).startsWith(2L).doesNotContain(1L);
        assertThat(recs.get(0).reason()).isEqualTo("Đã thuộc nhưng 2 ngày chưa ôn — ôn lại để không quên");
    }

    @Test
    @DisplayName("Mỗi từ kèm số lượt / lượt đúng / số ngày từ lần luyện gần nhất của người hỏi")
    void vocabularyItems_CarryPerWordStats() {
        Vocabulary a = vocab(1, "xin chào", greet, true);
        Vocabulary b = vocab(2, "tạm biệt", greet, true);
        LocalDateTime now = LocalDateTime.now();
        givenData(List.of(a, b), List.of(progress(a, LearningStatus.LEARNING, now.minusDays(3))),
                List.of(new Attempts(1L, 5L, 0L, now.minusDays(3), null)));

        List<VocabularyItem> items = service.getContext(7L).getVocabularies();

        assertThat(items.get(0)).extracting(VocabularyItem::attemptCount, VocabularyItem::correctCount,
                VocabularyItem::daysSinceLastPractice).containsExactly(5L, 0L, 3);
        assertThat(items.get(1)).extracting(VocabularyItem::attemptCount, VocabularyItem::daysSinceLastPractice)
                .containsExactly(0L, null);
    }

    @Test
    @DisplayName("Khoảng ôn: 1, 3, 7, 14, 30 ngày theo số lần ký đúng")
    void reviewIntervals() {
        assertThat(List.of(0L, 1L, 2L, 3L, 4L, 5L, 9L)).extracting(AssistantContextServiceImpl::reviewIntervalDays)
                .containsExactly(1, 1, 3, 7, 14, 30, 30);
    }
}
