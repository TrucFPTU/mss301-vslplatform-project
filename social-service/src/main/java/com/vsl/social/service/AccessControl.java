package com.vsl.social.service;

import com.vsl.common.exception.AppException;
import com.vsl.common.exception.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Objects;

/** Kiểm tra quyền dùng chung: ADMIN (từ JWT role) và chủ sở hữu tài nguyên. */
final class AccessControl {

    private static final String ROLE_ADMIN = "ROLE_ADMIN";

    private AccessControl() {
    }

    static boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> ROLE_ADMIN.equals(a.getAuthority()));
    }

    /** Cho qua nếu userId là một trong các ownerIds, hoặc là ADMIN; ngược lại FORBIDDEN. */
    static void requireOwnerOrAdmin(Long userId, Long... ownerIds) {
        for (Long ownerId : ownerIds) {
            if (Objects.equals(ownerId, userId)) {
                return;
            }
        }
        if (!isAdmin()) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }
    }

    static void requireOwner(Long userId, Long ownerId) {
        if (!Objects.equals(ownerId, userId)) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }
    }
}
