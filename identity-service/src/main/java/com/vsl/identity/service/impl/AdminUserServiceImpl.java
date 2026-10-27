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
import com.vsl.identity.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;

    /** Seed tài khoản Admin mặc định khi ứng dụng khởi chạy nếu chưa có tài khoản Admin nào */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seedDefaultAdmin() {
        // Chỉ seed cho hệ thống CHƯA có admin nào. Trước đây chỉ kiểm tra username "admin" nên khi DB đã có
        // admin khác dùng email admin@vsl.com (vd dữ liệu chuyển từ monolith) thì insert trùng email -> app không khởi động.
        if (userRepository.countByRole(Role.ADMIN) > 0) {
            return;
        }
        if (userRepository.existsByUsername("admin") || userRepository.existsByEmail("admin@vsl.com")) {
            log.warn("Không seed admin mặc định: username 'admin' hoặc email 'admin@vsl.com' đã được dùng.");
            return;
        }
        {
            User admin = User.builder()
                    .username("admin")
                    .email("admin@vsl.com")
                    .passwordHash(passwordEncoder.encode("admin123"))
                    .fullName("Hệ Thống Quản Trị Viên")
                    .role(Role.ADMIN)
                    .status(UserStatus.ACTIVE)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
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
        requireNotSelf(id, "Không thể tự đổi trạng thái tài khoản của chính mình");
        requireNotDeleted(user);
        if (request.getStatus() == UserStatus.DELETED) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Dùng chức năng xóa tài khoản để xóa người dùng");
        }
        if (user.getRole() == Role.ADMIN && request.getStatus() != UserStatus.ACTIVE) {
            requireAnotherActiveAdmin(id);
        }
        UserStatus oldStatus = user.getStatus();
        user.setStatus(request.getStatus());
        if (request.getStatus() != UserStatus.ACTIVE) {
            // Khóa tài khoản -> đăng xuất mọi thiết bị (refresh token không gia hạn được nữa).
            refreshTokenService.revokeAll(id);
        }

        User updated = userRepository.save(user);
        log.info("Admin changed user status: userId={} oldStatus={} newStatus={} reason={}",
                id, oldStatus, request.getStatus(), request.getReason());
        return AdminUserResponse.from(updated);
    }

    @Override
    @Transactional
    public AdminUserResponse updateUserRole(Long id, UserRoleUpdateRequest request) {
        User user = getUserOrThrow(id);
        requireNotSelf(id, "Không thể tự đổi quyền của chính mình");
        requireNotDeleted(user);
        if (user.getRole() == Role.ADMIN && request.getRole() != Role.ADMIN) {
            requireAnotherActiveAdmin(id);
        }
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

        Role role = request.getRole() != null ? request.getRole() : Role.USER;
        User user = User.builder()
                .username(username)
                .email(email)
                .passwordHash(passwordEncoder.encode(request.getPassword().trim()))
                .fullName(request.getFullName() != null ? request.getFullName().trim() : null)
                .role(role)
                .status(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        User saved = userRepository.save(user);
        log.info("Admin created new user: id={} username={} role={}", saved.getUserId(), saved.getUsername(), saved.getRole());
        return AdminUserResponse.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public UserStatisticsResponse getUserStatistics() {
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countByStatus(UserStatus.ACTIVE);
        long bannedUsers = userRepository.countByStatus(UserStatus.BANNED);
        long pendingUsers = userRepository.countByStatus(UserStatus.PENDING_VERIFICATION);
        long adminUsers = userRepository.countByRole(Role.ADMIN);
        long regularUsers = userRepository.countByRole(Role.USER);

        return UserStatisticsResponse.builder()
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                .bannedUsers(bannedUsers)
                .pendingUsers(pendingUsers)
                .adminUsers(adminUsers)
                .regularUsers(regularUsers)
                .build();
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        User user = getUserOrThrow(id);
        requireNotSelf(id, "Không thể tự xóa tài khoản admin của chính mình");
        requireNotDeleted(user);
        if (user.getRole() == Role.ADMIN) {
            requireAnotherActiveAdmin(id);
        }
        user.anonymize();
        userRepository.save(user);
        refreshTokenService.revokeAll(id);
        log.info("Admin xóa tài khoản userId={}", id);
    }

    /** Không cho admin tự khóa / hạ quyền / xóa chính mình (dễ tự khóa mất quyền quản trị). */
    private void requireNotSelf(Long targetId, String message) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Long actorId && actorId.equals(targetId)) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, message);
        }
    }

    private void requireNotDeleted(User user) {
        if (user.getStatus() == UserStatus.DELETED) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Tài khoản đã bị xóa");
        }
    }

    /** Luôn giữ ít nhất một admin đang hoạt động. */
    private void requireAnotherActiveAdmin(Long excludeId) {
        if (userRepository.countByRoleAndStatusAndUserIdNot(Role.ADMIN, UserStatus.ACTIVE, excludeId) == 0) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Hệ thống phải còn ít nhất một admin đang hoạt động");
        }
    }

    private User getUserOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Không tìm thấy người dùng với id: " + id));
    }
}
