package com.vsl.learning.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Tạo / sửa từ vựng (admin). Ảnh và video hướng dẫn đi qua endpoint upload riêng - không nhận URL ở đây
 * (bản trước ghi đè URL mỗi lần sửa, client không gửi lại là mất ảnh/video).
 */
@Data
public class VocabularyRequest {

    @NotNull(message = "categoryId không được để trống")
    private Long categoryId;

    @NotBlank(message = "Từ vựng không được để trống")
    @Size(max = 255, message = "Từ vựng tối đa 255 ký tự")
    private String word;

    @Size(max = 255, message = "Mô tả tối đa 255 ký tự")
    private String description;

    /** Chỉ số class trong model AI (0-999); mỗi class chỉ gắn với một từ. */
    @Min(value = 0, message = "expectedId phải trong khoảng 0-999")
    @Max(value = 999, message = "expectedId phải trong khoảng 0-999")
    private Integer expectedId;
}
