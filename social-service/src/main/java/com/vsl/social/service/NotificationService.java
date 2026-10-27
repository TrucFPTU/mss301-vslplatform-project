package com.vsl.social.service;

import com.vsl.common.response.PageResponse;
import com.vsl.social.dto.NotificationResponse;
import com.vsl.social.entity.NotificationType;

public interface NotificationService {

    /** Tạo thông báo cho recipientId. Bỏ qua nếu người nhận chính là người thực hiện. */
    void notify(Long recipientId, Long actorId, NotificationType type, Long blogId);

    PageResponse<NotificationResponse> list(Long userId, int page, int size);

    long unreadCount(Long userId);

    NotificationResponse markRead(Long notificationId, Long userId);

    int markAllRead(Long userId);
}
