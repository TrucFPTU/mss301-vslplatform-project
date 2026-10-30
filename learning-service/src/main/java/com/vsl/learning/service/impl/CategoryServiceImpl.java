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
import org.springframework.web.multipart.MultipartFile;

/**
 * Danh mục (bê từ CategoryServiceImpl monolith, thêm upload ảnh bìa).
 * Sửa: tên trùng so không phân biệt hoa/thường + bỏ khoảng trắng thừa ("Động vật" = "động vật ").
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final VocabularyRepository vocabularyRepository;
    private final MediaStorage mediaStorage;

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
        String name = request.getName().trim();
        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new AppException(ErrorCode.CATEGORY_NAME_TAKEN);
        }
        Category saved = categoryRepository.save(Category.builder()
                .name(name)
                .description(request.getDescription())
                .build());
        log.info("Admin created category id={}, name='{}'", saved.getId(), saved.getName());
        return toResponse(saved);
    }

    @Override
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = getCategoryOrThrow(id);
        String name = request.getName().trim();
        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new AppException(ErrorCode.CATEGORY_NAME_TAKEN);
        }
        category.setName(name);
        category.setDescription(request.getDescription());
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
        mediaStorage.deleteAfterCommit(MediaStorage.CATEGORY_IMAGE_BUCKET, category.getImageUrl());
        log.info("Admin deleted category id={}", id);
    }

    @Override
    public CategoryResponse uploadImage(Long id, MultipartFile image) {
        Category category = getCategoryOrThrow(id);
        String url = mediaStorage.uploadImage(MediaStorage.CATEGORY_IMAGE_BUCKET, "category-" + id, image);
        String old = category.getImageUrl();
        category.setImageUrl(url);
        Category saved = categoryRepository.save(category);
        mediaStorage.deleteAfterCommit(MediaStorage.CATEGORY_IMAGE_BUCKET, old);
        return toResponse(saved);
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
