package com.vsl.identity.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vsl.common.response.PageResponse;
import com.vsl.identity.dto.request.AdminCreateUserRequest;
import com.vsl.identity.dto.request.UserRoleUpdateRequest;
import com.vsl.identity.dto.request.UserStatusUpdateRequest;
import com.vsl.identity.dto.response.AdminUserResponse;
import com.vsl.identity.dto.response.UserStatisticsResponse;
import com.vsl.identity.entity.Role;
import com.vsl.identity.entity.UserStatus;
import com.vsl.identity.service.AdminUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminUserControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private AdminUserService adminUserService;

    @InjectMocks
    private AdminUserController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("GET /api/users/admin -> 200 OK và danh sách người dùng phân trang")
    void getUsers_Returns200() throws Exception {
        AdminUserResponse item = AdminUserResponse.builder()
                .id(1L)
                .username("nguyenvana")
                .email("vana@gmail.com")
                .fullName("Nguyễn Văn A")
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        PageResponse<AdminUserResponse> pageResponse = PageResponse.of(List.of(item), 0, 10, 1);

        when(adminUserService.getUsers(eq("vana"), eq(Role.USER), eq(UserStatus.ACTIVE), eq(0), eq(10)))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/api/users/admin")
                        .param("keyword", "vana")
                        .param("role", "USER")
                        .param("status", "ACTIVE")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.content[0].id").value(1L))
                .andExpect(jsonPath("$.data.content[0].username").value("nguyenvana"))
                .andExpect(jsonPath("$.data.totalElements").value(1L));

        verify(adminUserService).getUsers(eq("vana"), eq(Role.USER), eq(UserStatus.ACTIVE), eq(0), eq(10));
    }

    @Test
    @DisplayName("GET /api/users/admin/statistics -> 200 OK và số liệu thống kê")
    void getUserStatistics_Returns200() throws Exception {
        UserStatisticsResponse stats = UserStatisticsResponse.builder()
                .totalUsers(50L)
                .activeUsers(45L)
                .bannedUsers(3L)
                .pendingUsers(2L)
                .adminUsers(5L)
                .regularUsers(45L)
                .build();

        when(adminUserService.getUserStatistics()).thenReturn(stats);

        mockMvc.perform(get("/api/users/admin/statistics")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.totalUsers").value(50L))
                .andExpect(jsonPath("$.data.activeUsers").value(45L))
                .andExpect(jsonPath("$.data.bannedUsers").value(3L));

        verify(adminUserService).getUserStatistics();
    }

    @Test
    @DisplayName("GET /api/users/admin/{id} -> 200 OK và chi tiết người dùng")
    void getUserById_Returns200() throws Exception {
        AdminUserResponse user = AdminUserResponse.builder()
                .id(1L)
                .username("nguyenvana")
                .email("vana@gmail.com")
                .fullName("Nguyễn Văn A")
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .build();

        when(adminUserService.getUserById(1L)).thenReturn(user);

        mockMvc.perform(get("/api/users/admin/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value(1L))
                .andExpect(jsonPath("$.data.username").value("nguyenvana"));

        verify(adminUserService).getUserById(1L);
    }

    @Test
    @DisplayName("POST /api/users/admin -> 201 Created và tạo mới người dùng")
    void createUser_Returns201() throws Exception {
        AdminCreateUserRequest req = AdminCreateUserRequest.builder()
                .username("newadmin")
                .email("newadmin@vsl.com")
                .password("password123")
                .fullName("Admin Mới")
                .role(Role.ADMIN)
                .build();

        AdminUserResponse created = AdminUserResponse.builder()
                .id(10L)
                .username("newadmin")
                .email("newadmin@vsl.com")
                .role(Role.ADMIN)
                .status(UserStatus.ACTIVE)
                .build();

        when(adminUserService.createUser(any(AdminCreateUserRequest.class))).thenReturn(created);

        mockMvc.perform(post("/api/users/admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value(10L))
                .andExpect(jsonPath("$.message").value("Tạo tài khoản người dùng thành công"));

        verify(adminUserService).createUser(any(AdminCreateUserRequest.class));
    }

    @Test
    @DisplayName("PUT /api/users/admin/{id}/status -> 200 OK và cập nhật trạng thái khoá tài khoản")
    void updateUserStatus_Returns200() throws Exception {
        UserStatusUpdateRequest req = UserStatusUpdateRequest.builder()
                .status(UserStatus.BANNED)
                .reason("Spam quảng cáo")
                .build();

        AdminUserResponse updated = AdminUserResponse.builder()
                .id(1L)
                .username("nguyenvana")
                .status(UserStatus.BANNED)
                .build();

        when(adminUserService.updateUserStatus(eq(1L), any(UserStatusUpdateRequest.class))).thenReturn(updated);

        mockMvc.perform(put("/api/users/admin/{id}/status", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.status").value("BANNED"))
                .andExpect(jsonPath("$.message").value("Đã khoá tài khoản người dùng"));

        verify(adminUserService).updateUserStatus(eq(1L), any(UserStatusUpdateRequest.class));
    }

    @Test
    @DisplayName("PUT /api/users/admin/{id}/role -> 200 OK và cập nhật vai trò")
    void updateUserRole_Returns200() throws Exception {
        UserRoleUpdateRequest req = UserRoleUpdateRequest.builder()
                .role(Role.ADMIN)
                .build();

        AdminUserResponse updated = AdminUserResponse.builder()
                .id(1L)
                .username("nguyenvana")
                .role(Role.ADMIN)
                .build();

        when(adminUserService.updateUserRole(eq(1L), any(UserRoleUpdateRequest.class))).thenReturn(updated);

        mockMvc.perform(put("/api/users/admin/{id}/role", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.role").value("ADMIN"))
                .andExpect(jsonPath("$.message").value("Cập nhật quyền người dùng thành công"));

        verify(adminUserService).updateUserRole(eq(1L), any(UserRoleUpdateRequest.class));
    }
}
