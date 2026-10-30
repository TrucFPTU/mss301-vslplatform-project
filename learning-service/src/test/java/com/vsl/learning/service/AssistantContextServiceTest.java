package com.vsl.learning.service;

import com.vsl.learning.dto.response.AssistantContextResponse;
import com.vsl.learning.dto.response.AssistantContextResponse.Recommendation;
import com.vsl.learning.dto.response.PracticeStatsResponse;
import com.vsl.learning.entity.Category;
import com.vsl.learning.entity.LearningStatus;
import com.vsl.learning.entity.UserProgress;
import com.vsl.learning.entity.Vocabulary;
import com.vsl.learning.repository.CategoryRepository;
import com.vsl.learning.repository.UserProgressRepository;
import com.vsl.learning.repository.VocabularyRepository;
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

    private void givenData(List<Vocabulary> vocabs, List<UserProgress> progress) {
        when(categoryRepository.findAll(any(Sort.class))).thenReturn(List.of(greet, family));
        when(vocabularyRepository.findAllByOrderByIdAsc()).thenReturn(vocabs);
        when(userProgressRepository.findAllWithVocabularyByUserId(7L)).thenReturn(progress);
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
    @DisplayName("Từ đang học (LEARNING) lên trước, lâu chưa tập nhất trước; rồi tới chủ đề đang học dở")
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

        // Ôn: mẹ (3 ngày trước) rồi bố; từ mới: "Gia đình" đã đụng 2 từ > "Chào hỏi" 1 từ.
        assertThat(ctx.getRecommendations()).extracting(Recommendation::vocabularyId).containsExactly(4L, 3L, 5L, 2L);
        assertThat(ctx.getRecommendations().get(2).reason()).contains("đang học");
        assertThat(ctx.getCategories().get(1).learningCount()).isEqualTo(2);
        assertThat(ctx.getCategories().get(0).learnedCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Tối đa 5 gợi ý, trong đó tối đa 3 từ cần ôn")
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

        assertThat(recs).hasSize(5);
        assertThat(recs.stream().filter(r -> r.reason().contains("ôn lại")).count()).isEqualTo(3);
    }
}
