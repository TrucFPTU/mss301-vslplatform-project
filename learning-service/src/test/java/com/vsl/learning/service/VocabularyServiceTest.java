package com.vsl.learning.service;

import com.vsl.common.exception.AppException;
import com.vsl.common.exception.ErrorCode;
import com.vsl.learning.client.AiServiceClient;
import com.vsl.learning.dto.request.VocabularyRequest;
import com.vsl.learning.dto.response.VocabularyResponse;
import com.vsl.learning.dto.response.VocabularySynonymResponse;
import com.vsl.learning.entity.Category;
import com.vsl.learning.entity.Vocabulary;
import com.vsl.learning.repository.AttemptHistoryRepository;
import com.vsl.learning.repository.UserProgressRepository;
import com.vsl.learning.repository.VocabularyRepository;
import com.vsl.learning.service.impl.MediaStorage;
import com.vsl.learning.service.impl.VocabularyServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class VocabularyServiceTest {

    @Mock
    VocabularyRepository vocabularyRepository;
    @Mock
    UserProgressRepository userProgressRepository;
    @Mock
    AttemptHistoryRepository attemptHistoryRepository;
    @Mock
    CategoryService categoryService;
    @Mock
    MediaStorage mediaStorage;
    @Mock
    AiServiceClient aiServiceClient;
    @InjectMocks
    VocabularyServiceImpl service;

    private final Category animals = Category.builder().id(1L).name("Động vật").build();

    private Vocabulary vocab(long id, String word, Integer expectedId) {
        Vocabulary v = new Vocabulary();
        v.setId(id);
        v.setWord(word);
        v.setCategory(animals);
        v.setExpectedId(expectedId);
        return v;
    }

    private VocabularyRequest request(String word, Integer expectedId) {
        VocabularyRequest r = new VocabularyRequest();
        r.setCategoryId(1L);
        r.setWord(word);
        r.setExpectedId(expectedId);
        return r;
    }

    @BeforeEach
    void setUp() {
        when(categoryService.getCategoryOrThrow(1L)).thenReturn(animals);
        when(vocabularyRepository.findAllBy(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(vocab(5L, "con chuột", 7))));
        when(vocabularyRepository.save(any(Vocabulary.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private static ErrorCode code(Runnable r) {
        try {
            r.run();
        } catch (AppException e) {
            return e.getErrorCode();
        }
        return null;
    }

    @Test
    void create_duplicateWordIgnoringAccents_rejected() {
        assertThat(code(() -> service.create(request("Con  Chuot", null)))).isEqualTo(ErrorCode.VOCABULARY_ALREADY_EXISTS);
        verify(vocabularyRepository, never()).save(any());
    }

    @Test
    void update_sameWordOnItself_allowed() {
        when(vocabularyRepository.findWithCategoryById(5L)).thenReturn(Optional.of(vocab(5L, "con chuột", 7)));

        VocabularyResponse res = service.update(5L, request("con chuột", 7));

        assertThat(res.getWord()).isEqualTo("con chuột");
    }

    @Test
    void create_sharedExpectedId_allowed() {
        // Một ký hiệu có thể mang nhiều nghĩa -> hai từ được chung class AI.
        VocabularyResponse res = service.create(request("con mèo", 7));

        assertThat(res.getExpectedId()).isEqualTo(7);
    }

    @Test
    void create_expectedIdOutOfRange_rejected() {
        assertThat(code(() -> service.create(request("con mèo", 1000)))).isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void uploadTutorialVideo_transcodesStoresAndDeletesOld() {
        Vocabulary v = vocab(5L, "con chuột", 7);
        v.setVideoTutorialUrl("http://minio/tutorial-videos/old.mp4");
        when(vocabularyRepository.findWithCategoryById(5L)).thenReturn(Optional.of(v));
        MockMultipartFile video = new MockMultipartFile("video", "a.mov", "video/quicktime", new byte[]{1, 2});
        when(aiServiceClient.transcode(video)).thenReturn(new byte[]{9, 9});
        when(mediaStorage.uploadMp4(eq(MediaStorage.TUTORIAL_BUCKET), eq("vocabulary-5"), any()))
                .thenReturn("http://minio/tutorial-videos/new.mp4");

        VocabularyResponse res = service.uploadTutorialVideo(5L, video, null);

        assertThat(res.getVideoTutorialUrl()).isEqualTo("http://minio/tutorial-videos/new.mp4");
        verify(mediaStorage).deleteAfterCommit(MediaStorage.TUTORIAL_BUCKET, "http://minio/tutorial-videos/old.mp4");
    }

    @Test
    void uploadTutorialVideo_rejectsNonVideo() {
        when(vocabularyRepository.findWithCategoryById(5L)).thenReturn(Optional.of(vocab(5L, "con chuột", 7)));

        assertThatThrownBy(() -> service.uploadTutorialVideo(5L,
                new MockMultipartFile("video", "x.exe", "application/octet-stream", new byte[]{1}), null))
                .isInstanceOf(AppException.class);
        verify(aiServiceClient, never()).transcode(any());
    }

    @Test
    void findSynonyms_aiFailure_reportsAiError() {
        when(aiServiceClient.findSynonyms(anyString(), any())).thenReturn(null);

        VocabularySynonymResponse res = service.findSynonyms("con chuột nhắt");

        assertThat(res.aiChecked()).isTrue();
        assertThat(res.aiError()).isTrue();
    }

    @Test
    void checkWordExists_foundIgnoringAccents() {
        assertThat(service.checkWordExists("CON CHUOT").exists()).isTrue();
        assertThat(service.checkWordExists("con mèo").exists()).isFalse();
    }
}
