package com.vsl.identity.service.impl;

import com.vsl.common.exception.AppException;
import com.vsl.common.exception.ErrorCode;
import com.vsl.common.response.PageResponse;
import com.vsl.common.storage.StorageService;
import com.vsl.common.storage.UploadValidator;
import com.vsl.identity.dto.ChangePasswordRequest;
import com.vsl.identity.dto.PublicUserStatsResponse;
import com.vsl.identity.dto.UpdateProfileRequest;
import com.vsl.identity.dto.UserDto;
import com.vsl.identity.dto.UserSummaryResponse;
import com.vsl.identity.entity.Role;
import com.vsl.identity.entity.User;
import com.vsl.identity.entity.UserStatus;
import com.vsl.identity.repository.UserRepository;
import com.vsl.identity.service.RefreshTokenService;
import com.vsl.identity.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final int MAX_SUMMARY_IDS = 100;

    static final String AVATAR_BUCKET = "avatars";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    /** Không cấu hình MinIO thì không có bean -> upload avatar báo lỗi, phần còn lại vẫn chạy. */
    private final ObjectProvider<StorageService> storage;

    @Override
    @Transactional
    public UserDto uploadAvatar(Long userId, MultipartFile image) {
        User user = findUserById(userId);
        String ext = UploadValidator.requireImage(image);
        StorageService s = storage.getIfAvailable();
        if (s == null) {
            throw new AppException(ErrorCode.FILE_UPLOAD_ERROR, "Chưa cấu hình kho lưu trữ ảnh (MinIO)");
        }
        String url;
        try (InputStream in = image.getInputStream()) {
            url = s.upload(AVATAR_BUCKET, UploadValidator.objectName("user-" + userId, ext), in,
                    image.getSize(), image.getContentType());
        } catch (IOException e) {
            throw new AppException(ErrorCode.INVALID_FILE, "Không đọc được file ảnh");
        }
        String old = user.getAvatarUrl();
        user.setAvatarUrl(url);
        UserDto dto = mapToDto(userRepository.save(user));
        deleteAvatarAfterCommit(old);
        return dto;
    }

    @Override
    @Transactional
    public UserDto removeAvatar(Long userId) {
        User user = findUserById(userId);
        String old = user.getAvatarUrl();
        user.setAvatarUrl(null);
        UserDto dto = mapToDto(userRepository.save(user));
        deleteAvatarAfterCommit(old);
        return dto;
    }

    @Override
    @Transactional
    public void deleteMyAccount(Long userId, String password) {
        User user = findUserById(userId);
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new AppException(ErrorCode.INVALID_CREDENTIALS, "Mật khẩu không đúng");
        }
        String avatar = user.getAvatarUrl();
        user.anonymize();
        userRepository.save(user);
        refreshTokenService.revokeAll(userId);
        deleteAvatarAfterCommit(avatar);
        log.info("Người dùng tự xóa tài khoản: userId={}", userId);
    }

    /** Xóa ảnh trên MinIO sau khi transaction commit (rollback thì DB không trỏ vào file đã mất). */
    private void deleteAvatarAfterCommit(String url) {
        StorageService s = storage.getIfAvailable();
        if (s == null || url == null) {
            return;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    s.deleteByUrl(AVATAR_BUCKET, url);
                }
            });
        } else {
            s.deleteByUrl(AVATAR_BUCKET, url);
        }
    }

    @Override
    @Transactional
    public UserDto updateNotificationSettings(Long userId, boolean emailNotificationsEnabled) {
        User user = findUserById(userId);
        user.setEmailNotificationsEnabled(emailNotificationsEnabled);
        return mapToDto(userRepository.save(user));
    }

    @Override
    @Transactional(readOnly = true)
    public PublicUserStatsResponse getPublicStats() {
        return new PublicUserStatsResponse(userRepository.count(), userRepository.countByRole(Role.USER));
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto getProfile(Long userId) {
        User user = findUserById(userId);
        return mapToDto(user);
    }

    @Override
    @Transactional
    public UserDto updateProfile(Long userId, UpdateProfileRequest request) {
        User user = findUserById(userId);

        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            user.setFullName(request.getFullName().trim());
        }
        if (request.getBio() != null) {
            user.setBio(request.getBio().trim());
        }

        user = userRepository.save(user);
        log.info("Cập nhật thông tin thành công cho userId={}", userId);
        return mapToDto(user);
    }

    @Override
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = findUserById(userId);

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new AppException(ErrorCode.INVALID_CREDENTIALS, "Mật khẩu hiện tại không chính xác");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        log.info("Đổi mật khẩu thành công cho userId={}", userId);
    }

    @Override
    @Transactional(readOnly = true)
    public UserSummaryResponse getPublicProfile(Long userId) {
        return UserSummaryResponse.from(findUserById(userId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserSummaryResponse> getSummaries(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        if (userIds.size() > MAX_SUMMARY_IDS) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Tối đa " + MAX_SUMMARY_IDS + " id mỗi lần tra cứu");
        }
        return userRepository.findAllById(new HashSet<>(userIds)).stream()
                .map(UserSummaryResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserSummaryResponse> searchUsers(String keyword, int page, int size) {
        String term = keyword == null ? "" : keyword.trim();
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);
        if (term.isEmpty()) {
            return PageResponse.of(List.of(), safePage, safeSize, 0);
        }
        Page<User> result = userRepository.searchByNameOrUsername(term, UserStatus.ACTIVE,
                PageRequest.of(safePage, safeSize, Sort.by("fullName").ascending().and(Sort.by("username"))));
        return PageResponse.of(result.getContent().stream().map(UserSummaryResponse::from).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements());
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Không tìm thấy người dùng với ID: " + userId));
    }

    private UserDto mapToDto(User user) {
        return UserDto.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .bio(user.getBio())
                .role(user.getRole())
                .active(user.isActive())
                .emailNotificationsEnabled(user.isEmailNotificationsEnabled())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
