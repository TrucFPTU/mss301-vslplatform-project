package com.vsl.identity.dto.response;

import lombok.Builder;

@Builder
public record UserStatisticsResponse(
        long totalUsers,
        long activeUsers,
        long bannedUsers,
        long pendingUsers,
        long adminUsers,
        long regularUsers
) {
}
