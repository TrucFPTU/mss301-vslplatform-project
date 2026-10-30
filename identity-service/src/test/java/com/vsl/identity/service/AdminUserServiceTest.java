package com.vsl.identity.service;

import com.vsl.common.exception.AppException;
import com.vsl.common.exception.ErrorCode;
import com.vsl.common.response.PageResponse;
import com.vsl.identity.dto.request.AdminCreateUserRequest;
import com.vsl.identity.dto.request.UserRoleUpdateRequest;
import com.vsl.identity.dto.request.UserStatusUpdateRequest;
import com.vsl.identity.dto.response.AdminUserResponse;
import com.vsl.identity.dto.response.UserStatisticsResponse;
import com.vsl.identity.entity.Role;
import com.vsl.identity.entity.User;
import com.vsl.identity.entity.UserStatus;
import com.vsl.identity.repository.UserRepository;
import com.vsl.identity.service.impl.AdminUserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.never;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private AdminUserServiceImpl adminUserService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .userId(1L)
                .username("nguyenvana")
                .email("vana@gmail.com")
                .passwordHash("encoded_pass")
                .fullName("Nguyễn Văn A")
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("getUsers -> trả về danh sách phân trang thành công")
    void getUsers_Success() {
        when(userRepository.searchUsers(eq("vana"), eq(Role.USER), eq(UserStatus.ACTIVE), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(sampleUser)));

        PageResponse<AdminUserResponse> result = adminUserService.getUsers("vana", Role.USER, UserStatus.ACTIVE, 0, 10);

        assertThat(result.getContent()).hasSize(1);
        AdminUserResponse item = result.getContent().get(0);
        assertThat(item.id()).isEqualTo(1L);
        assertThat(item.username()).isEqualTo("nguyenvana");
        assertThat(item.email()).isEqualTo("vana@gmail.com");
    }

    @Test
    @DisplayName("getUserById -> trả về chi tiết người dùng khi tồn tại")
    void getUserById_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));

        AdminUserResponse result = adminUserService.getUserById(1L);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.fullName()).isEqualTo("Nguyễn Văn A");
    }

    @Test
    @DisplayName("getUserById -> ném NOT_FOUND khi không tồn tại")
    void getUserById_NotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminUserService.getUserById(99L))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_FOUND);
    }

    @Test
    @DisplayName("updateUserStatus -> cập nhật trạng thái khoá tài khoản BANNED thành công")
    void updateUserStatus_Ban_Success() {
        UserStatusUpdateRequest req = UserStatusUpdateRequest.builder()
                .status(UserStatus.BANNED)
                .reason("Vi phạm chính sách bài viết nhiều lần")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        AdminUserResponse result = adminUserService.updateUserStatus(1L, req);

        assertThat(result.status()).isEqualTo(UserStatus.BANNED);
        verify(userRepository).save(sampleUser);
    }

    @Test
    @DisplayName("updateUserRole -> nâng quyền tài khoản thành ROLE_ADMIN thành công")
    void updateUserRole_Success() {
        UserRoleUpdateRequest req = UserRoleUpdateRequest.builder()
                .role(Role.ADMIN)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        AdminUserResponse result = adminUserService.updateUserRole(1L, req);

        assertThat(result.role()).isEqualTo(Role.ADMIN);
        verify(userRepository).save(sampleUser);
    }

    @Test
    @DisplayName("createUser -> tạo mới tài khoản thành công")
    void createUser_Success() {
        AdminCreateUserRequest req = AdminCreateUserRequest.builder()
                .username("newadmin")
                .email("admin@test.com")
                .password("secret123")
                .fullName("Quản trị mới")
                .role(Role.ADMIN)
                .build();

        when(userRepository.existsByUsername("newadmin")).thenReturn(false);
        when(userRepository.existsByEmail("admin@test.com")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("hashed_secret");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setUserId(2L);
            return u;
        });

        AdminUserResponse result = adminUserService.createUser(req);

        assertThat(result.id()).isEqualTo(2L);
        assertThat(result.username()).isEqualTo("newadmin");
        assertThat(result.role()).isEqualTo(Role.ADMIN);
        assertThat(result.status()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    @DisplayName("createUser -> ném VALIDATION_ERROR khi trùng username")
    void createUser_DuplicateUsername() {
        AdminCreateUserRequest req = AdminCreateUserRequest.builder()
                .username("nguyenvana")
                .email("new@test.com")
                .password("secret123")
                .build();

        when(userRepository.existsByUsername("nguyenvana")).thenReturn(true);

        assertThatThrownBy(() -> adminUserService.createUser(req))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VALIDATION_ERROR);
    }

    @Test
    @DisplayName("createUser -> ném VALIDATION_ERROR khi trùng email")
    void createUser_DuplicateEmail() {
        AdminCreateUserRequest req = AdminCreateUserRequest.builder()
                .username("newuser")
                .email("vana@gmail.com")
                .password("secret123")
                .build();

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("vana@gmail.com")).thenReturn(true);

        assertThatThrownBy(() -> adminUserService.createUser(req))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VALIDATION_ERROR);
    }

    @Test
    @DisplayName("getUserStatistics -> trả về đầy đủ các số liệu thống kê người dùng")
    void getUserStatistics_Success() {
        when(userRepository.count()).thenReturn(50L);
        when(userRepository.countByStatus(UserStatus.ACTIVE)).thenReturn(45L);
        when(userRepository.countByStatus(UserStatus.BANNED)).thenReturn(3L);
        when(userRepository.countByStatus(UserStatus.PENDING_VERIFICATION)).thenReturn(2L);
        when(userRepository.countByRole(Role.ADMIN)).thenReturn(5L);
        when(userRepository.countByRole(Role.USER)).thenReturn(45L);

        UserStatisticsResponse stats = adminUserService.getUserStatistics();

        assertThat(stats.totalUsers()).isEqualTo(50L);
        assertThat(stats.activeUsers()).isEqualTo(45L);
        assertThat(stats.bannedUsers()).isEqualTo(3L);
        assertThat(stats.pendingUsers()).isEqualTo(2L);
        assertThat(stats.adminUsers()).isEqualTo(5L);
        assertThat(stats.regularUsers()).isEqualTo(45L);
    }

    @Test
    @DisplayName("Khóa user -> thu hồi mọi refresh token")
    void updateUserStatus_Ban_RevokesTokens() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        UserStatusUpdateRequest req = UserStatusUpdateRequest.builder().status(UserStatus.BANNED).reason("spam").build();

        adminUserService.updateUserStatus(1L, req);

        verify(refreshTokenService).revokeAll(1L);
    }

    @Test
    @DisplayName("Không đặt DELETED qua API đổi trạng thái")
    void updateUserStatus_DeletedRejected() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        UserStatusUpdateRequest req = UserStatusUpdateRequest.builder().status(UserStatus.DELETED).build();

        assertThatThrownBy(() -> adminUserService.updateUserStatus(1L, req)).isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("Không hạ quyền admin cuối cùng")
    void updateUserRole_LastAdminRejected() {
        sampleUser.setRole(Role.ADMIN);
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(userRepository.countByRoleAndStatusAndUserIdNot(Role.ADMIN, UserStatus.ACTIVE, 1L)).thenReturn(0L);
        UserRoleUpdateRequest req = UserRoleUpdateRequest.builder().role(Role.USER).build();

        assertThatThrownBy(() -> adminUserService.updateUserRole(1L, req)).isInstanceOf(AppException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Admin không tự xóa chính mình")
    void deleteUser_SelfRejected() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(1L, null, java.util.List.of()));
        try {
            assertThatThrownBy(() -> adminUserService.deleteUser(1L)).isInstanceOf(AppException.class);
        } finally {
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
        }
    }

    @Test
    @DisplayName("Xóa user -> ẩn danh hóa + thu hồi token")
    void deleteUser_Anonymizes() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));

        adminUserService.deleteUser(1L);

        assertThat(sampleUser.getStatus()).isEqualTo(UserStatus.DELETED);
        verify(refreshTokenService).revokeAll(1L);
    }
}
