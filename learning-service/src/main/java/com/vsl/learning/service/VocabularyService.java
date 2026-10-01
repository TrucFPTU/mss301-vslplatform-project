package com.vsl.learning.service;

import com.vsl.common.exception.AppException;
import com.vsl.common.exception.ErrorCode;
import com.vsl.common.response.PageResponse;
import com.vsl.learning.dto.request.VocabularyRequest;
import com.vsl.learning.dto.response.VocabularyResponse;
import com.vsl.learning.entity.Category;
import com.vsl.learning.entity.Vocabulary;
import com.vsl.learning.repository.AttemptHistoryRepository;
import com.vsl.learning.repository.UserProgressRepository;
import com.vsl.learning.repository.VocabularyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class VocabularyService {

    private final VocabularyRepository vocabularyRepository;
    private final UserProgressRepository userProgressRepository;
    private final AttemptHistoryRepository attemptHistoryRepository;
    private final CategoryService categoryService;

    @Transactional(readOnly = true)
    public PageResponse<VocabularyResponse> getAll(int page, int size) {
        return Paging.toPageResponse(vocabularyRepository.findAllBy(pageable(page, size))
                .map(VocabularyService::toResponse));
    }

    @Transactional(readOnly = true)
    public VocabularyResponse getById(Long id) {
        return toResponse(getVocabularyOrThrow(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<VocabularyResponse> search(String keyword, int page, int size) {
        if (keyword == null || keyword.isBlank()) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Từ khoá tìm kiếm không được để trống");
        }
        return Paging.toPageResponse(vocabularyRepository
                .findByWordContainingIgnoreCase(keyword.trim(), pageable(page, size))
                .map(VocabularyService::toResponse));
    }

    @Transactional(readOnly = true)
    public PageResponse<VocabularyResponse> getByCategory(Long categoryId, int page, int size) {
        categoryService.getCategoryOrThrow(categoryId);
        return Paging.toPageResponse(vocabularyRepository.findByCategoryId(categoryId, pageable(page, size))
                .map(VocabularyService::toResponse));
    }

    public VocabularyResponse create(VocabularyRequest request) {
        Vocabulary vocabulary = new Vocabulary();
        apply(vocabulary, request);
        vocabulary = vocabularyRepository.save(vocabulary);
        log.info("Admin created vocabulary id={}, word='{}'", vocabulary.getId(), vocabulary.getWord());
        return toResponse(vocabulary);
    }

    public VocabularyResponse update(Long id, VocabularyRequest request) {
        Vocabulary vocabulary = getVocabularyOrThrow(id);
        apply(vocabulary, request);
        vocabulary = vocabularyRepository.save(vocabulary);
        log.info("Admin updated vocabulary id={}, word='{}'", vocabulary.getId(), vocabulary.getWord());
        return toResponse(vocabulary);
    }

    public void delete(Long id) {
        Vocabulary vocabulary = getVocabularyOrThrow(id);
        if (attemptHistoryRepository.existsByVocabulary_Id(id) || userProgressRepository.existsByVocabulary_Id(id)) {
            throw new AppException(ErrorCode.VALIDATION_ERROR,
                    "Không thể xoá từ vựng vì đã có lịch sử luyện tập hoặc tiến trình học gắn với từ này");
        }
        vocabularyRepository.delete(vocabulary);
        log.info("Admin deleted vocabulary id={}", id);
    }

    Vocabulary getVocabularyOrThrow(Long id) {
        return vocabularyRepository.findWithCategoryById(id)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Không tìm thấy từ vựng"));
    }

    private void apply(Vocabulary vocabulary, VocabularyRequest request) {
        Category category = categoryService.getCategoryOrThrow(request.getCategoryId());
        vocabulary.setCategory(category);
        vocabulary.setWord(request.getWord());
        vocabulary.setDescription(request.getDescription());
        vocabulary.setVideoTutorialUrl(request.getVideoTutorialUrl());
        vocabulary.setImageUrl(request.getImageUrl());
        vocabulary.setExpectedId(request.getExpectedId());
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
