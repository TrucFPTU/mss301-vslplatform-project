package com.vsl.learning.service.impl;

import com.vsl.common.exception.AppException;
import com.vsl.common.exception.ErrorCode;
import com.vsl.common.response.PageResponse;
import com.vsl.learning.dto.request.CategoryRequest;
import com.vsl.learning.dto.response.CategoryResponse;
import com.vsl.learning.entity.Category;
import com.vsl.learning.repository.CategoryRepository;
import com.vsl.learning.repository.VocabularyRepository;
import com.vsl.learning.service.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final VocabularyRepository vocabularyRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CategoryResponse> getAll(int page, int size) {
        return Paging.toPageResponse(categoryRepository
                .findAll(PageRequest.of(Paging.safePage(page), Paging.safeSize(size), Sort.by("id")))
                .map(CategoryServiceImpl::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getById(Long id) {
        return toResponse(getCategoryOrThrow(id));
    }

    @Override
    public CategoryResponse create(CategoryRequest request) {
        if (categoryRepository.existsByName(request.getName())) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Tên danh mục đã tồn tại");
        }
        Category saved = categoryRepository.save(Category.builder()
                .name(request.getName())
                .description(request.getDescription())
                .imageUrl(request.getImageUrl())
                .build());
        log.info("Admin created category id={}, name='{}'", saved.getId(), saved.getName());
        return toResponse(saved);
    }

    @Override
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = getCategoryOrThrow(id);
        if (!request.getName().equals(category.getName()) && categoryRepository.existsByName(request.getName())) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Tên danh mục đã tồn tại");
        }
        category.setName(request.getName());
        category.setDescription(request.getDescription());
        category.setImageUrl(request.getImageUrl());
        return toResponse(categoryRepository.save(category));
    }

    @Override
    public void delete(Long id) {
        Category category = getCategoryOrThrow(id);
        if (vocabularyRepository.existsByCategoryId(id)) {
            throw new AppException(ErrorCode.VALIDATION_ERROR,
                    "Không thể xoá danh mục vì vẫn còn từ vựng nằm trong danh mục này");
        }
        categoryRepository.delete(category);
        log.info("Admin deleted category id={}", id);
    }

    @Override
    public Category getCategoryOrThrow(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Không tìm thấy danh mục"));
    }

    static CategoryResponse toResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .imageUrl(category.getImageUrl())
                .build();
    }
}
