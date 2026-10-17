package com.vsl.learning.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AchievementRequest {

    @NotBlank(message = "Mã thành tựu (key) không được để trống")
    @Size(max = 50, message = "Mã thành tựu tối đa 50 ký tự")
    private String key;

    @NotBlank(message = "Tên thành tựu không được để trống")
    @Size(max = 100, message = "Tên thành tựu tối đa 100 ký tự")
    private String name;

    @NotBlank(message = "Mô tả thành tựu không được để trống")
    @Size(max = 255, message = "Mô tả tối đa 255 ký tự")
    private String description;

    @Size(max = 50, message = "Mã icon tối đa 50 ký tự")
    private String iconKey;
}
