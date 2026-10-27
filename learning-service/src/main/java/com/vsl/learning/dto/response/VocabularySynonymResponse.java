package com.vsl.learning.dto.response;

import java.util.List;

/**
 * Từ đồng nghĩa đã có trong kho (AI).
 * aiChecked=false: AI đang tắt -> UI không hiện gì; aiError=true: gọi AI lỗi -> UI báo, không chặn form.
 */
public record VocabularySynonymResponse(boolean aiChecked, boolean aiError, List<SynonymItem> synonyms) {

    public record SynonymItem(String word, String categoryName) {
    }
}
