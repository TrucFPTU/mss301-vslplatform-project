package com.vsl.social.dto;

import com.vsl.social.entity.NotificationType;

import java.time.Instant;

public record NotificationResponse(
        Long id,
        NotificationType type,
        UserBriefResponse actor,
        Long blogId,
        String blogTitle,
        Long commentId,
        Long replyId,
        boolean read,
        Instant createdAt
) {
}
