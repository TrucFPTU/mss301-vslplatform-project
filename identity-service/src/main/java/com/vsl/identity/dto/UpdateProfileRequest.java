package com.vsl.identity.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {

    @Size(max = 100, message = "Họ và tên tối đa 100 ký tự")
    private String fullName;

    @Size(max = 500, message = "Tiểu sử tối đa 500 ký tự")
    private String bio;

    @Size(max = 1024, message = "Đường dẫn ảnh đại diện tối đa 1024 ký tự")
    private String avatarUrl;
}
