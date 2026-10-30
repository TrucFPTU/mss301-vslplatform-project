package com.vsl.identity.service;

import com.vsl.common.response.PageResponse;
import com.vsl.identity.dto.request.AdminCreateUserRequest;
import com.vsl.identity.dto.request.UserRoleUpdateRequest;
import com.vsl.identity.dto.request.UserStatusUpdateRequest;
import com.vsl.identity.dto.response.AdminUserResponse;
import com.vsl.identity.dto.response.UserStatisticsResponse;
import com.vsl.identity.entity.Role;
import com.vsl.identity.entity.UserStatus;

public interface AdminUserService {

    PageResponse<AdminUserResponse> getUsers(String keyword, Role role, UserStatus status, int page, int size);

    AdminUserResponse getUserById(Long id);

    AdminUserResponse updateUserStatus(Long id, UserStatusUpdateRequest request);

    AdminUserResponse updateUserRole(Long id, UserRoleUpdateRequest request);

    AdminUserResponse createUser(AdminCreateUserRequest request);

    UserStatisticsResponse getUserStatistics();

    /** Xóa (ẩn danh hóa) tài khoản. Không xóa được chính mình. */
    void deleteUser(Long id);
}
