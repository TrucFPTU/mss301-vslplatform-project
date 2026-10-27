package com.vsl.identity.dto;

import com.vsl.identity.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {

    private Long userId;
    private String username;
    private String email;
    private String fullName;
    private String avatarUrl;
    private String bio;
    private Role role;
    private boolean active;
    /** Đã bật email nhắc nhở streak chưa (mặc định tắt). */
    private boolean emailNotificationsEnabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
