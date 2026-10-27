package com.vsl.identity.service.impl;

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
import com.vsl.identity.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /** Seed tài khoản Admin mặc định khi ứng dụng khởi chạy nếu chưa có tài khoản Admin nào */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seedDefaultAdmin() {
        if (!userRepository.existsByUsername("admin")) {
            User admin = User.builder()
                    .username("admin")
                    .email("admin@vsl.com")
                    .password(passwordEncoder.encode("admin123"))
                    .fullName("Hệ Thống Quản Trị Viên")
                    .role(Role.ROLE_ADMIN)
                    .status(UserStatus.ACTIVE)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
            userRepository.save(admin);
            log.info("Default Admin account seeded: username=admin");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminUserResponse> getUsers(String keyword, Role role, UserStatus status, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = size <= 0 ? 10 : Math.min(size, 100);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        String safeKeyword = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;
        Page<User> userPage = userRepository.searchUsers(safeKeyword, role, status, pageable);

        List<AdminUserResponse> content = userPage.getContent().stream()
                .map(AdminUserResponse::from)
                .toList();

        return PageResponse.of(content, userPage.getNumber(), userPage.getSize(), userPage.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public AdminUserResponse getUserById(Long id) {
        User user = getUserOrThrow(id);
        return AdminUserResponse.from(user);
    }

    @Override
    @Transactional
    public AdminUserResponse updateUserStatus(Long id, UserStatusUpdateRequest request) {
        User user = getUserOrThrow(id);
        UserStatus oldStatus = user.getStatus();
        user.setStatus(request.getStatus());

        User updated = userRepository.save(user);
        log.info("Admin changed user status: userId={} oldStatus={} newStatus={} reason={}",
                id, oldStatus, request.getStatus(), request.getReason());
        return AdminUserResponse.from(updated);
    }

    @Override
    @Transactional
    public AdminUserResponse updateUserRole(Long id, UserRoleUpdateRequest request) {
        User user = getUserOrThrow(id);
        Role oldRole = user.getRole();
        user.setRole(request.getRole());

        User updated = userRepository.save(user);
        log.info("Admin changed user role: userId={} oldRole={} newRole={}", id, oldRole, request.getRole());
        return AdminUserResponse.from(updated);
    }

    @Override
    @Transactional
    public AdminUserResponse createUser(AdminCreateUserRequest request) {
        String username = request.getUsername().trim();
        if (userRepository.existsByUsername(username)) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Tên đăng nhập đã tồn tại: " + username);
        }

        String email = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Email đã tồn tại: " + email);
        }

        Role role = request.getRole() != null ? request.getRole() : Role.ROLE_USER;
        User user = User.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(request.getPassword().trim()))
                .fullName(request.getFullName() != null ? request.getFullName().trim() : null)
                .role(role)
                .status(UserStatus.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        User saved = userRepository.save(user);
        log.info("Admin created new user: id={} username={} role={}", saved.getId(), saved.getUsername(), saved.getRole());
        return AdminUserResponse.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public UserStatisticsResponse getUserStatistics() {
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countByStatus(UserStatus.ACTIVE);
        long bannedUsers = userRepository.countByStatus(UserStatus.BANNED);
        long pendingUsers = userRepository.countByStatus(UserStatus.PENDING_VERIFICATION);
        long adminUsers = userRepository.countByRole(Role.ROLE_ADMIN);
        long regularUsers = userRepository.countByRole(Role.ROLE_USER);

        return UserStatisticsResponse.builder()
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                .bannedUsers(bannedUsers)
                .pendingUsers(pendingUsers)
                .adminUsers(adminUsers)
                .regularUsers(regularUsers)
                .build();
    }

    private User getUserOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Không tìm thấy người dùng với id: " + id));
    }
}
