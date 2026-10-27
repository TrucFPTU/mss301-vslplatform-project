package com.vsl.learning.service.impl;

import com.vsl.common.exception.AppException;
import com.vsl.common.exception.ErrorCode;
import com.vsl.common.response.PageResponse;
import com.vsl.common.storage.UploadValidator;
import com.vsl.common.text.VietnameseText;
import com.vsl.learning.client.AiServiceClient;
import com.vsl.learning.dto.request.VocabularyRequest;
import com.vsl.learning.dto.response.VocabularyExistsResponse;
import com.vsl.learning.dto.response.VocabularyResponse;
import com.vsl.learning.dto.response.VocabularySynonymResponse;
import com.vsl.learning.entity.Category;
import com.vsl.learning.entity.Vocabulary;
import com.vsl.learning.repository.AttemptHistoryRepository;
import com.vsl.learning.repository.UserProgressRepository;
import com.vsl.learning.repository.VocabularyRepository;
import com.vsl.learning.service.CategoryService;
import com.vsl.learning.service.VocabularyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Từ vựng (bê đủ từ VocabularyServiceImpl monolith: kiểm tra từ, từ đồng nghĩa AI, upload ảnh/video).
 * Sửa so với bản cũ: admin tạo/sửa cũng bị chặn trùng từ (bỏ dấu) như luồng đề xuất; expectedId 0-999 và
 * không trùng (2 từ cùng class AI thì chấm điểm không phân biệt được); thay video thì xóa video cũ.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class VocabularyServiceImpl implements VocabularyService {

    private final VocabularyRepository vocabularyRepository;
    private final UserProgressRepository userProgressRepository;
    private final AttemptHistoryRepository attemptHistoryRepository;
    private final CategoryService categoryService;
    private final MediaStorage mediaStorage;
    private final AiServiceClient aiServiceClient;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<VocabularyResponse> getAll(int page, int size) {
        return Paging.toPageResponse(vocabularyRepository.findAllBy(pageable(page, size))
                .map(VocabularyServiceImpl::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public VocabularyResponse getById(Long id) {
        return toResponse(getVocabularyOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<VocabularyResponse> search(String keyword, int page, int size) {
        if (keyword == null || keyword.isBlank()) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Từ khoá tìm kiếm không được để trống");
        }
        return Paging.toPageResponse(vocabularyRepository
                .findByWordContainingIgnoreCase(keyword.trim(), pageable(page, size))
                .map(VocabularyServiceImpl::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<VocabularyResponse> getByCategory(Long categoryId, int page, int size) {
        categoryService.getCategoryOrThrow(categoryId);
        return Paging.toPageResponse(vocabularyRepository.findByCategoryId(categoryId, pageable(page, size))
                .map(VocabularyServiceImpl::toResponse));
    }

    @Override
    public VocabularyResponse create(VocabularyRequest request) {
        Vocabulary vocabulary = new Vocabulary();
        apply(vocabulary, request, null);
        vocabulary = vocabularyRepository.save(vocabulary);
        log.info("Admin created vocabulary id={}, word='{}'", vocabulary.getId(), vocabulary.getWord());
        return toResponse(vocabulary);
    }

    @Override
    public VocabularyResponse update(Long id, VocabularyRequest request) {
        Vocabulary vocabulary = getVocabularyOrThrow(id);
        apply(vocabulary, request, id);
        vocabulary = vocabularyRepository.save(vocabulary);
        log.info("Admin updated vocabulary id={}, word='{}'", vocabulary.getId(), vocabulary.getWord());
        return toResponse(vocabulary);
    }

    @Override
    public void delete(Long id) {
        Vocabulary vocabulary = getVocabularyOrThrow(id);
        if (attemptHistoryRepository.existsByVocabulary_Id(id) || userProgressRepository.existsByVocabulary_Id(id)) {
            throw new AppException(ErrorCode.VALIDATION_ERROR,
                    "Không thể xoá từ vựng vì đã có lịch sử luyện tập hoặc tiến trình học gắn với từ này");
        }
        vocabularyRepository.delete(vocabulary);
        mediaStorage.deleteAfterCommit(MediaStorage.TUTORIAL_BUCKET, vocabulary.getVideoTutorialUrl());
        mediaStorage.deleteAfterCommit(MediaStorage.VOCABULARY_IMAGE_BUCKET, vocabulary.getImageUrl());
        log.info("Admin deleted vocabulary id={}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public VocabularyExistsResponse checkWordExists(String word) {
        return findMatching(word, null)
                .map(v -> new VocabularyExistsResponse(true, v.getWord(),
                        v.getCategory() != null ? v.getCategory().getId() : null,
                        v.getCategory() != null ? v.getCategory().getName() : null))
                .orElseGet(VocabularyExistsResponse::none);
    }

    @Override
    @Transactional(readOnly = true)
    public VocabularySynonymResponse findSynonyms(String word) {
        String normalized = VietnameseText.normalize(word);
        if (normalized.isBlank()) {
            return new VocabularySynonymResponse(false, false, List.of());
        }
        List<AiServiceClient.VocabEntry> existing = vocabularyRepository.findAllBy(Pageable.unpaged()).stream()
                .map(v -> new AiServiceClient.VocabEntry(v.getWord(),
                        v.getCategory() != null ? v.getCategory().getName() : ""))
                .toList();
        AiServiceClient.SynonymResult result = aiServiceClient.findSynonyms(normalized, existing);
        if (result == null) {
            // Gọi AI thất bại (vd hết quota) -> báo lỗi để UI hiện thông báo, không chặn form.
            return new VocabularySynonymResponse(true, true, List.of());
        }
        List<VocabularySynonymResponse.SynonymItem> items = result.synonyms() == null ? List.of()
                : result.synonyms().stream()
                .map(s -> new VocabularySynonymResponse.SynonymItem(s.word(), s.categoryName()))
                .toList();
        return new VocabularySynonymResponse(result.checked(), false, items);
    }

    @Override
    public VocabularyResponse uploadImage(Long id, MultipartFile image) {
        Vocabulary vocabulary = getVocabularyOrThrow(id);
        String url = mediaStorage.uploadImage(MediaStorage.VOCABULARY_IMAGE_BUCKET, "vocabulary-" + id, image);
        String old = vocabulary.getImageUrl();
        vocabulary.setImageUrl(url);
        Vocabulary saved = vocabularyRepository.save(vocabulary);
        mediaStorage.deleteAfterCommit(MediaStorage.VOCABULARY_IMAGE_BUCKET, old);
        log.info("Admin uploaded image for vocabularyId={}", id);
        return toResponse(saved);
    }

    @Override
    public VocabularyResponse uploadTutorialVideo(Long id, MultipartFile video, Integer expectedId) {
        Vocabulary vocabulary = getVocabularyOrThrow(id);
        UploadValidator.requireVideo(video);
        if (expectedId != null) {
            requireFreeExpectedId(expectedId, id);
        }
        byte[] mp4 = aiServiceClient.transcode(video);
        String url = mediaStorage.uploadMp4(MediaStorage.TUTORIAL_BUCKET, "vocabulary-" + id, mp4);
        String old = vocabulary.getVideoTutorialUrl();
        vocabulary.setVideoTutorialUrl(url);
        if (expectedId != null) {
            vocabulary.setExpectedId(expectedId);
        }
        Vocabulary saved = vocabularyRepository.save(vocabulary);
        mediaStorage.deleteAfterCommit(MediaStorage.TUTORIAL_BUCKET, old);
        log.info("Admin uploaded tutorial video for vocabularyId={}", id);
        return toResponse(saved);
    }

    Vocabulary getVocabularyOrThrow(Long id) {
        return vocabularyRepository.findWithCategoryById(id)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Không tìm thấy từ vựng"));
    }

    private void apply(Vocabulary vocabulary, VocabularyRequest request, Long selfId) {
        Category category = categoryService.getCategoryOrThrow(request.getCategoryId());
        String word = VietnameseText.normalize(request.getWord());
        if (findMatching(word, selfId).isPresent()) {
            throw new AppException(ErrorCode.VOCABULARY_ALREADY_EXISTS);
        }
        if (request.getExpectedId() != null) {
            requireFreeExpectedId(request.getExpectedId(), selfId);
        }
        vocabulary.setCategory(category);
        vocabulary.setWord(word);
        vocabulary.setDescription(request.getDescription());
        vocabulary.setExpectedId(request.getExpectedId());
    }

    private void requireFreeExpectedId(Integer expectedId, Long selfId) {
        if (expectedId < 0 || expectedId > 999) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "expectedId phải trong khoảng 0-999");
        }
        boolean taken = selfId == null
                ? vocabularyRepository.existsByExpectedId(expectedId)
                : vocabularyRepository.existsByExpectedIdAndIdNot(expectedId, selfId);
        if (taken) {
            throw new AppException(ErrorCode.VOCABULARY_EXPECTED_ID_TAKEN);
        }
    }

    /** Tìm từ trùng - bỏ dấu, không phân biệt hoa/thường (bỏ qua chính từ đang sửa). */
    private Optional<Vocabulary> findMatching(String word, Long excludeId) {
        String target = VietnameseText.fold(word);
        if (target.isBlank()) {
            return Optional.empty();
        }
        return vocabularyRepository.findAllBy(Pageable.unpaged()).stream()
                .filter(v -> !Objects.equals(v.getId(), excludeId))
                .filter(v -> VietnameseText.fold(v.getWord()).equals(target))
                .findFirst();
    }

    private static Pageable pageable(int page, int size) {
        return PageRequest.of(Paging.safePage(page), Paging.safeSize(size), Sort.by("id"));
    }

    static VocabularyResponse toResponse(Vocabulary vocabulary) {
        Category category = vocabulary.getCategory();
        return VocabularyResponse.builder()
                .id(vocabulary.getId())
                .categoryId(category != null ? category.getId() : null)
                .categoryName(category != null ? category.getName() : null)
                .word(vocabulary.getWord())
                .description(vocabulary.getDescription())
                .videoTutorialUrl(vocabulary.getVideoTutorialUrl())
                .imageUrl(vocabulary.getImageUrl())
                .expectedId(vocabulary.getExpectedId())
                .build();
    }
}
