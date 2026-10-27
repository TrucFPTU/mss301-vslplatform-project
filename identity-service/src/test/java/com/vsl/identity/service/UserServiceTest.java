package com.vsl.identity.service;

import com.vsl.common.exception.AppException;
import com.vsl.identity.dto.ChangePasswordRequest;
import com.vsl.identity.dto.UpdateProfileRequest;
import com.vsl.identity.dto.UserDto;
import com.vsl.identity.entity.Role;
import com.vsl.identity.entity.User;
import com.vsl.identity.entity.UserStatus;
import com.vsl.identity.repository.UserRepository;
import com.vsl.identity.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private org.springframework.beans.factory.ObjectProvider<com.vsl.common.storage.StorageService> storage;

    @InjectMocks
    private UserServiceImpl userService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .userId(1L)
                .username("testuser")
                .email("test@example.com")
                .fullName("Test User")
                .passwordHash("hashed_password")
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .build();
    }

    @Test
    @DisplayName("Lấy hồ sơ cá nhân thành công")
    void getProfile_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));

        UserDto dto = userService.getProfile(1L);

        assertNotNull(dto);
        assertEquals(1L, dto.getUserId());
        assertEquals("testuser", dto.getUsername());
    }

    @Test
    @DisplayName("Lấy hồ sơ ném AppException khi không tìm thấy user")
    void getProfile_NotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(AppException.class, () -> userService.getProfile(99L));
    }

    @Test
    @DisplayName("Cập nhật hồ sơ cá nhân thành công")
    void updateProfile_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .fullName("New Full Name")
                .bio("My bio")
                .build();

        UserDto updated = userService.updateProfile(1L, request);

        assertEquals("New Full Name", updated.getFullName());
        assertEquals("My bio", updated.getBio());
    }

    @Test
    @DisplayName("Đổi mật khẩu thành công")
    void changePassword_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("old_pass", "hashed_password")).thenReturn(true);
        when(passwordEncoder.encode("new_pass")).thenReturn("new_hashed_password");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("old_pass")
                .newPassword("new_pass")
                .build();

        assertDoesNotThrow(() -> userService.changePassword(1L, request));
        verify(userRepository, times(1)).save(sampleUser);
    }

    @Test
    @DisplayName("Đổi mật khẩu thất bại khi mật khẩu cũ sai")
    void changePassword_WrongOldPassword() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("wrong_old_pass", "hashed_password")).thenReturn(false);

        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("wrong_old_pass")
                .newPassword("new_pass")
                .build();

        assertThrows(AppException.class, () -> userService.changePassword(1L, request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Tự xóa tài khoản sai mật khẩu -> không xóa")
    void deleteMyAccount_WrongPassword() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("wrong", "hashed_password")).thenReturn(false);

        assertThrows(AppException.class, () -> userService.deleteMyAccount(1L, "wrong"));
        assertEquals(UserStatus.ACTIVE, sampleUser.getStatus());
        verify(refreshTokenService, never()).revokeAll(any());
    }

    @Test
    @DisplayName("Tự xóa tài khoản: ẩn danh hóa, giải phóng email, thu hồi mọi phiên")
    void deleteMyAccount_AnonymizesAndRevokes() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("pass", "hashed_password")).thenReturn(true);

        userService.deleteMyAccount(1L, "pass");

        assertEquals(UserStatus.DELETED, sampleUser.getStatus());
        assertEquals("deleted+1@deleted.local", sampleUser.getEmail());
        assertEquals("Người dùng đã xóa", sampleUser.getFullName());
        assertFalse(sampleUser.isActive());
        verify(refreshTokenService).revokeAll(1L);
    }

    @Test
    @DisplayName("Upload avatar không phải ảnh -> lỗi, không lưu")
    void uploadAvatar_RejectsNonImage() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));

        assertThrows(AppException.class, () -> userService.uploadAvatar(1L,
                new org.springframework.mock.web.MockMultipartFile("image", "a.svg", "image/svg+xml", new byte[]{1})));
        verify(userRepository, never()).save(any());
    }
}
