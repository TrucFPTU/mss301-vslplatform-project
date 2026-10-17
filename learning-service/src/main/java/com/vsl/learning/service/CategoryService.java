package com.vsl.learning.service;

import com.vsl.common.response.PageResponse;
import com.vsl.learning.dto.request.CategoryRequest;
import com.vsl.learning.dto.response.CategoryResponse;
import com.vsl.learning.entity.Category;

public interface CategoryService {

    PageResponse<CategoryResponse> getAll(int page, int size);

    CategoryResponse getById(Long id);

    CategoryResponse create(CategoryRequest request);

    CategoryResponse update(Long id, CategoryRequest request);

    void delete(Long id);

    Category getCategoryOrThrow(Long id);
}
