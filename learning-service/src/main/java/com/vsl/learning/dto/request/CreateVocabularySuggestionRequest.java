package com.vsl.learning.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Người dùng đề xuất một từ vựng mới: danh mục + tên từ vựng (+ mô tả tùy chọn). */
@Data
public class CreateVocabularySuggestionRequest {

    @NotNull(message = "Vui lòng chọn danh mục")
    private Long categoryId;

    @NotBlank(message = "Vui lòng nhập tên từ vựng")
    @Size(max = 255, message = "Tên từ vựng tối đa 255 ký tự")
    private String word;

    @Size(max = 255, message = "Mô tả tối đa 255 ký tự")
    private String description;
}
