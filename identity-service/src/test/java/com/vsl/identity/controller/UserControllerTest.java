package com.vsl.identity.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vsl.common.exception.GlobalExceptionHandler;
import com.vsl.identity.dto.ChangePasswordRequest;
import com.vsl.identity.dto.UpdateProfileRequest;
import com.vsl.identity.dto.UserDto;
import com.vsl.identity.dto.UserSummaryResponse;
import com.vsl.identity.entity.Role;
import com.vsl.identity.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        // Mock @AuthenticationPrincipal Long userId resolver
        HandlerMethodArgumentResolver authPrincipalResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter,
                                          ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest,
                                          WebDataBinderFactory binderFactory) {
                return 1L; // Mock userId = 1L
            }
        };

        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setCustomArgumentResolvers(authPrincipalResolver)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("API GET /api/users/me thành công")
    void getMyProfile_Success() throws Exception {
        UserDto userDto = UserDto.builder()
                .userId(1L)
                .username("testuser")
                .email("test@example.com")
                .fullName("Test User")
                .role(Role.USER)
                .active(true)
                .build();

        when(userService.getProfile(1L)).thenReturn(userDto);

        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(1L))
                .andExpect(jsonPath("$.data.username").value("testuser"));
    }

    @Test
    @DisplayName("API PUT /api/users/me thành công")
    void updateMyProfile_Success() throws Exception {
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .fullName("Updated Name")
                .bio("New Bio")
                .build();

        UserDto userDto = UserDto.builder()
                .userId(1L)
                .username("testuser")
                .email("test@example.com")
                .fullName("Updated Name")
                .bio("New Bio")
                .role(Role.USER)
                .active(true)
                .build();

        when(userService.updateProfile(eq(1L), any(UpdateProfileRequest.class))).thenReturn(userDto);

        mockMvc.perform(put("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.fullName").value("Updated Name"));
    }

    @Test
    @DisplayName("API PUT /api/users/me/password thành công")
    void changePassword_Success() throws Exception {
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("old_pass123")
                .newPassword("new_pass123")
                .build();

        doNothing().when(userService).changePassword(eq(1L), any(ChangePasswordRequest.class));

        mockMvc.perform(put("/api/users/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Đổi mật khẩu thành công"));
    }

    @Test
    @DisplayName("API GET /api/users/{id} trả hồ sơ công khai, KHÔNG lộ email/role")
    void getPublicProfile_Success() throws Exception {
        when(userService.getPublicProfile(2L))
                .thenReturn(new UserSummaryResponse(2L, "anotheruser", "Another User", null));

        mockMvc.perform(get("/api/users/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(2L))
                .andExpect(jsonPath("$.data.username").value("anotheruser"))
                .andExpect(jsonPath("$.data.email").doesNotExist())
                .andExpect(jsonPath("$.data.role").doesNotExist());
    }

    @Test
    @DisplayName("API GET /api/users/summaries?ids=... tra cứu hàng loạt")
    void getSummaries_Success() throws Exception {
        when(userService.getSummaries(java.util.List.of(1L, 2L))).thenReturn(java.util.List.of(
                new UserSummaryResponse(1L, "a", "A", null), new UserSummaryResponse(2L, "b", "B", null)));

        mockMvc.perform(get("/api/users/summaries").param("ids", "1", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[1].fullName").value("B"));
    }
}
