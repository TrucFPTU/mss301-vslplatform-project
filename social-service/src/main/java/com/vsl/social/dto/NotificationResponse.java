package com.vsl.social.dto;

import com.vsl.social.entity.BlogNotification;
import com.vsl.social.entity.NotificationType;

import java.time.Instant;

public record NotificationResponse(
        Long id,
        Long actorId,
        NotificationType type,
        Long blogId,
        boolean read,
        Instant createdAt
) {
    public static NotificationResponse from(BlogNotification n) {
        return new NotificationResponse(n.getId(), n.getActorId(), n.getType(), n.getBlogId(),
                n.isRead(), n.getCreatedAt());
    }
}
