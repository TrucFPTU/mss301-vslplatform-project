package com.vsl.identity.service;

import com.vsl.common.response.PageResponse;
import com.vsl.identity.dto.ChangePasswordRequest;
import com.vsl.identity.dto.UserSummaryResponse;

import org.springframework.web.multipart.MultipartFile;

import java.util.Collection;
import java.util.List;
import com.vsl.identity.dto.PublicUserStatsResponse;
import com.vsl.identity.dto.UpdateProfileRequest;
import com.vsl.identity.dto.UserDto;

public interface UserService {

    UserDto getProfile(Long userId);

    UserDto updateProfile(Long userId, UpdateProfileRequest request);

    void changePassword(Long userId, ChangePasswordRequest request);

    /** Hồ sơ công khai (không có email) - ai cũng xem được. */
    UserSummaryResponse getPublicProfile(Long userId);

    /** Tra cứu hàng loạt hồ sơ công khai (tối đa 100 id; id không tồn tại bị bỏ qua). */
    List<UserSummaryResponse> getSummaries(Collection<Long> userIds);

    /** Tìm người dùng đang hoạt động theo họ tên / username. */
    PageResponse<UserSummaryResponse> searchUsers(String keyword, int page, int size);

    /** Upload avatar (JPEG/PNG/WEBP/GIF, 5MB) lên MinIO; xóa ảnh cũ. */
    UserDto uploadAvatar(Long userId, MultipartFile image);

    UserDto removeAvatar(Long userId);

    /** Tự xóa tài khoản: xác nhận mật khẩu, ẩn danh hóa, thu hồi mọi phiên đăng nhập. */
    void deleteMyAccount(Long userId, String password);

    /** Bật/tắt email nhắc nhở streak của chính người dùng. */
    UserDto updateNotificationSettings(Long userId, boolean emailNotificationsEnabled);

    /** Tổng người dùng + tổng học viên (role USER). */
    PublicUserStatsResponse getPublicStats();
}
