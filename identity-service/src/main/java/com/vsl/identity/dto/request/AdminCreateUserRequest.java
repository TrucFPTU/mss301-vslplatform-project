package com.vsl.identity.dto.request;

import com.vsl.identity.entity.Role;
import jakarta.validation.constraints.Email;
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
public class AdminCreateUserRequest {

    @NotBlank(message = "Tên đăng nhập không được để trống")
    @Size(min = 3, max = 50, message = "Tên đăng nhập từ 3 đến 50 ký tự")
    private String username;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    @Size(max = 100, message = "Email tối đa 100 ký tự")
    private String email;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 6, max = 50, message = "Mật khẩu tối thiểu 6 ký tự")
    private String password;

    @Size(max = 100, message = "Họ và tên tối đa 100 ký tự")
    private String fullName;

    private Role role;
}
