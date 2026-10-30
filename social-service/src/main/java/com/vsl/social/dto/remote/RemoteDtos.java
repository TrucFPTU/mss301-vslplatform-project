package com.vsl.social.dto.remote;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/** Dữ liệu đọc về từ identity-service / ai-service (chỉ các trường social cần). */
public final class RemoteDtos {

    private RemoteDtos() {
    }

    /** Envelope ApiResponse của service khác. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RemoteResponse<T>(boolean success, String message, T data) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RemotePage<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
    }

    /** Hồ sơ công khai từ identity-service. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record UserSummary(Long userId, String username, String fullName, String avatarUrl) {

        /** Tên hiển thị: họ tên nếu có, không thì username (giống monolith). */
        public String displayName() {
            return fullName != null && !fullName.isBlank() ? fullName : username;
        }
    }

    public record ModerationRequest(String title, String content) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ModerationResult(boolean allowed, String reason, boolean checked) {
    }

    public record ExistingBlog(Long id, String title, String content) {
    }

    public record DuplicateCheckRequest(String title, String content, List<ExistingBlog> existing) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DuplicateCheckResult(boolean duplicate, Long matchedBlogId, String reason, boolean checked) {
    }
}
