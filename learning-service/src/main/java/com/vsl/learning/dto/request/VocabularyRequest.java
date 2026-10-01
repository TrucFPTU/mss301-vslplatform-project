package com.vsl.learning.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class VocabularyRequest {

    @NotNull(message = "categoryId không được để trống")
    private Long categoryId;

    @NotBlank(message = "Từ vựng không được để trống")
    @Size(max = 255, message = "Từ vựng tối đa 255 ký tự")
    private String word;

    @Size(max = 255, message = "Mô tả tối đa 255 ký tự")
    private String description;

    @Size(max = 500, message = "URL video tối đa 500 ký tự")
    private String videoTutorialUrl;

    @Size(max = 500, message = "URL ảnh tối đa 500 ký tự")
    private String imageUrl;

    /** Chỉ số class trong model AI (0-999). */
    @Min(value = 0, message = "expectedId phải >= 0")
    private Integer expectedId;
}
