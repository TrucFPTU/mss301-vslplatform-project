package com.vsl.learning.service;

import com.vsl.common.response.PageResponse;
import com.vsl.learning.dto.request.VocabularyRequest;
import com.vsl.learning.dto.response.VocabularyResponse;

public interface VocabularyService {

    PageResponse<VocabularyResponse> getAll(int page, int size);

    VocabularyResponse getById(Long id);

    PageResponse<VocabularyResponse> search(String keyword, int page, int size);

    PageResponse<VocabularyResponse> getByCategory(Long categoryId, int page, int size);

    VocabularyResponse create(VocabularyRequest request);

    VocabularyResponse update(Long id, VocabularyRequest request);

    void delete(Long id);
}
