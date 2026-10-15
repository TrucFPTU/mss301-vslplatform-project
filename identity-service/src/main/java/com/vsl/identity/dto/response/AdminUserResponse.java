package com.vsl.identity.dto.response;

import com.vsl.identity.entity.Role;
import com.vsl.identity.entity.User;
import com.vsl.identity.entity.UserStatus;
import lombok.Builder;

import java.time.Instant;

@Builder
public record AdminUserResponse(
        Long id,
        String username,
        String email,
        String fullName,
        String avatarUrl,
        Role role,
        UserStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public static AdminUserResponse from(User u) {
        return AdminUserResponse.builder()
                .id(u.getId())
                .username(u.getUsername())
                .email(u.getEmail())
                .fullName(u.getFullName())
                .avatarUrl(u.getAvatarUrl())
                .role(u.getRole())
                .status(u.getStatus())
                .createdAt(u.getCreatedAt())
                .updatedAt(u.getUpdatedAt())
                .build();
    }
}
