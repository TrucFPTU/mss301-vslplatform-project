package com.vsl.learning.service;

import com.vsl.learning.entity.AttemptHistory;
import com.vsl.learning.entity.LearningStatus;
import com.vsl.learning.entity.UserProgress;
import com.vsl.learning.entity.Vocabulary;
import com.vsl.learning.repository.AttemptHistoryRepository;
import com.vsl.learning.repository.UserProgressRepository;
import com.vsl.learning.service.impl.AttemptServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttemptServiceTest {

    @Mock
    private AttemptHistoryRepository attemptHistoryRepository;
    @Mock
    private UserProgressRepository userProgressRepository;
    @Mock
    private AchievementService achievementService;

    @InjectMocks
    private AttemptServiceImpl service;

    private final Vocabulary vocab = Vocabulary.builder().id(5L).word("xin chào").expectedId(10).build();

    @Test
    @DisplayName("Lượt GẦN ĐÚNG: lưu mức ALMOST_CORRECT, không tính là đúng, tiến độ = đang học")
    void recordAttempt_AlmostCorrect() {
        when(userProgressRepository.findByUserIdAndVocabulary_Id(7L, 5L)).thenReturn(Optional.empty());

        service.recordAttempt(7L, vocab, "ALMOST_CORRECT", 11, 30.5);

        ArgumentCaptor<AttemptHistory> saved = ArgumentCaptor.forClass(AttemptHistory.class);
        verify(attemptHistoryRepository).save(saved.capture());
        assertThat(saved.getValue().getResultStatus()).isEqualTo("ALMOST_CORRECT");
        assertThat(saved.getValue().getIsCorrect()).isFalse();
        ArgumentCaptor<UserProgress> progress = ArgumentCaptor.forClass(UserProgress.class);
        verify(userProgressRepository).save(progress.capture());
        assertThat(progress.getValue().getLearningStatus()).isEqualTo(LearningStatus.LEARNING);
    }

    @Test
    @DisplayName("Lượt ĐÚNG: isCorrect = true, tiến độ = đã thuộc")
    void recordAttempt_Correct() {
        when(userProgressRepository.findByUserIdAndVocabulary_Id(7L, 5L)).thenReturn(Optional.empty());

        service.recordAttempt(7L, vocab, "CORRECT", 10, 88.0);

        ArgumentCaptor<AttemptHistory> saved = ArgumentCaptor.forClass(AttemptHistory.class);
        verify(attemptHistoryRepository).save(saved.capture());
        assertThat(saved.getValue().getIsCorrect()).isTrue();
        ArgumentCaptor<UserProgress> progress = ArgumentCaptor.forClass(UserProgress.class);
        verify(userProgressRepository).save(progress.capture());
        assertThat(progress.getValue().getLearningStatus()).isEqualTo(LearningStatus.LEARNED);
    }

    @Test
    @DisplayName("Lịch sử trả mức chấm; lượt cũ chưa có mức thì suy từ isCorrect")
    void recentAttempts_StatusWithLegacyFallback() {
        AttemptHistory almost = AttemptHistory.builder().id(1L).vocabulary(vocab).isCorrect(false)
                .resultStatus("ALMOST_CORRECT").build();
        AttemptHistory oldOk = AttemptHistory.builder().id(2L).vocabulary(vocab).isCorrect(true).build();
        AttemptHistory oldFail = AttemptHistory.builder().id(3L).vocabulary(vocab).isCorrect(false).build();
        when(attemptHistoryRepository.findPageByUserId(eq(7L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(almost, oldOk, oldFail)));

        assertThat(service.getRecentAttempts(7L, 10)).extracting("status")
                .containsExactly("ALMOST_CORRECT", "CORRECT", "INCORRECT");
    }
}
