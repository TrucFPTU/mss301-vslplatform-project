package com.vsl.learning.dto.response;

/** Từ đã có trong kho chưa (so khớp bỏ dấu, không phân biệt hoa/thường) - dùng khi đề xuất từ mới. */
public record VocabularyExistsResponse(boolean exists, String word, Long categoryId, String categoryName) {

    public static VocabularyExistsResponse none() {
        return new VocabularyExistsResponse(false, null, null, null);
    }
}
