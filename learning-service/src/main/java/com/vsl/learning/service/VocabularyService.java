package com.vsl.learning.service;

import com.vsl.common.response.PageResponse;
import com.vsl.learning.dto.request.VocabularyRequest;
import com.vsl.learning.dto.response.VocabularyExistsResponse;
import com.vsl.learning.dto.response.VocabularyResponse;
import com.vsl.learning.dto.response.VocabularySynonymResponse;
import org.springframework.web.multipart.MultipartFile;

public interface VocabularyService {

    PageResponse<VocabularyResponse> getAll(int page, int size);

    VocabularyResponse getById(Long id);

    PageResponse<VocabularyResponse> search(String keyword, int page, int size);

    PageResponse<VocabularyResponse> getByCategory(Long categoryId, int page, int size);

    /** Tạo từ (admin). Chặn trùng từ (bỏ dấu) và trùng expectedId. */
    VocabularyResponse create(VocabularyRequest request);

    VocabularyResponse update(Long id, VocabularyRequest request);

    /** Xóa từ chưa có lịch sử luyện tập; xóa luôn ảnh + video trên MinIO. */
    void delete(Long id);

    /** Từ đã có chưa (bỏ dấu, không phân biệt hoa/thường). */
    VocabularyExistsResponse checkWordExists(String word);

    /** Từ đồng nghĩa trong kho, dùng AI (Gemini ở ai-service). */
    VocabularySynonymResponse findSynonyms(String word);

    /** Upload ảnh minh họa (thay ảnh cũ). */
    VocabularyResponse uploadImage(Long id, MultipartFile image);

    /** Upload video hướng dẫn: chuyển mã H.264 ở ai-service rồi lưu; expectedId tùy chọn (gán class AI). */
    VocabularyResponse uploadTutorialVideo(Long id, MultipartFile video, Integer expectedId);
}
