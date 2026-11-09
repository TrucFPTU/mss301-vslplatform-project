package com.vsl.identity.service;

import com.vsl.identity.dto.RegisterRequest;
import com.vsl.identity.dto.request.AdminCreateUserRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class UsernameValidationTest {

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    private static boolean usernameValid(String username) {
        RegisterRequest req = RegisterRequest.builder().username(username).email("a@b.vn").password("secret12")
                .fullName("Người Học").otp("123456").build();
        return VALIDATOR.validateProperty(req, "username").isEmpty();
    }

    @ParameterizedTest
    @DisplayName("Tên đăng nhập hợp lệ: chữ không dấu, số, . _ -")
    @ValueSource(strings = {"minhanh", "admin01", "nguyen.van_a", "tran-b", "ABC"})
    void valid(String username) {
        assertThat(usernameValid(username)).isTrue();
    }

    @ParameterizedTest
    @DisplayName("Tên đăng nhập không hợp lệ: khoảng trắng, có dấu, ký tự đặc biệt, quá ngắn")
    @ValueSource(strings = {"ab cd", "Tuấn Phạm", "user!", "a@b", "ab", "<script>"})
    void invalid(String username) {
        assertThat(usernameValid(username)).isFalse();
    }

    @ParameterizedTest
    @DisplayName("Admin tạo tài khoản cũng áp dụng cùng quy tắc")
    @ValueSource(strings = {"ab cd", "Tuấn"})
    void adminCreate_Invalid(String username) {
        AdminCreateUserRequest req = new AdminCreateUserRequest();
        req.setUsername(username);
        assertThat(VALIDATOR.validateProperty(req, "username")).isNotEmpty();
    }
}
