package com.vsl.learning.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Tạo / sửa danh mục (admin). Ảnh bìa đi qua POST /api/categories/{id}/image. */
@Data
public class CategoryRequest {

    @NotBlank(message = "Tên danh mục không được để trống")
    @Size(max = 255, message = "Tên danh mục tối đa 255 ký tự")
    private String name;

    @Size(max = 255, message = "Mô tả tối đa 255 ký tự")
    private String description;
}
