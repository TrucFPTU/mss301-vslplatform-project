package com.vsl.learning.service;

import com.vsl.common.exception.AppException;
import com.vsl.common.exception.ErrorCode;
import com.vsl.learning.dto.response.VocabularyResponse;
import com.vsl.learning.entity.Category;
import com.vsl.learning.entity.UserFavorite;
import com.vsl.learning.entity.Vocabulary;
import com.vsl.learning.repository.UserFavoriteRepository;
import com.vsl.learning.repository.VocabularyRepository;
import com.vsl.learning.service.impl.FavoriteServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FavoriteServiceTest {

    @Mock
    UserFavoriteRepository favoriteRepository;
    @Mock
    VocabularyRepository vocabularyRepository;
    @InjectMocks
    FavoriteServiceImpl service;

    private static Vocabulary vocab(long id, String word) {
        return Vocabulary.builder().id(id).word(word).expectedId((int) id)
                .category(Category.builder().id(1L).name("Gia đình").build()).build();
    }

    @Test
    void getMine_mapsVocabulariesInRepositoryOrder() {
        when(favoriteRepository.findAllWithVocabularyByUserId(9L)).thenReturn(List.of(
                UserFavorite.builder().id(2L).userId(9L).vocabulary(vocab(12L, "mẹ")).createdAt(LocalDateTime.now()).build(),
                UserFavorite.builder().id(1L).userId(9L).vocabulary(vocab(11L, "bố")).createdAt(LocalDateTime.now().minusDays(1)).build()));

        List<VocabularyResponse> res = service.getMine(9L);

        assertThat(res).extracting(VocabularyResponse::getId).containsExactly(12L, 11L);
        assertThat(res.get(0).getWord()).isEqualTo("mẹ");
        assertThat(res.get(0).getCategoryName()).isEqualTo("Gia đình");
    }

    @Test
    void add_existingVocabulary_insertsIfAbsent() {
        when(vocabularyRepository.existsById(12L)).thenReturn(true);

        service.add(9L, 12L);

        verify(favoriteRepository).insertIfAbsent(eq(9L), eq(12L), any(LocalDateTime.class));
    }

    @Test
    void add_unknownVocabulary_throwsNotFound() {
        when(vocabularyRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> service.add(9L, 999L))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
        verify(favoriteRepository, never()).insertIfAbsent(anyLong(), anyLong(), any());
    }

    @Test
    void remove_deletesOnlyThatUsersFavorite() {
        service.remove(9L, 12L);

        verify(favoriteRepository).deleteByUserIdAndVocabularyId(9L, 12L);
    }
}
