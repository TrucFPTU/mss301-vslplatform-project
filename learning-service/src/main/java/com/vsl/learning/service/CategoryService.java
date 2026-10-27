package com.vsl.learning.service;

import com.vsl.common.response.PageResponse;
import com.vsl.learning.dto.request.CategoryRequest;
import com.vsl.learning.dto.response.CategoryResponse;
import com.vsl.learning.entity.Category;
import org.springframework.web.multipart.MultipartFile;

public interface CategoryService {

    PageResponse<CategoryResponse> getAll(int page, int size);

    CategoryResponse getById(Long id);

    CategoryResponse create(CategoryRequest request);

    CategoryResponse update(Long id, CategoryRequest request);

    /** Xóa danh mục không còn từ vựng; xóa luôn ảnh bìa. */
    void delete(Long id);

    /** Upload ảnh bìa (thay ảnh cũ). */
    CategoryResponse uploadImage(Long id, MultipartFile image);

    Category getCategoryOrThrow(Long id);
}
