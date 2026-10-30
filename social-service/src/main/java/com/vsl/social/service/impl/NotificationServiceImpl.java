package com.vsl.social.service.impl;

import com.vsl.common.exception.AppException;
import com.vsl.common.exception.ErrorCode;
import com.vsl.common.response.PageResponse;
import com.vsl.social.dto.NotificationResponse;
import com.vsl.social.entity.BlogNotification;
import com.vsl.social.entity.NotificationType;
import com.vsl.social.repository.BlogNotificationRepository;
import com.vsl.social.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final BlogNotificationRepository notificationRepository;
    private final SocialViews views;

    @Override
    @Transactional
    public void notify(Long recipientId, Long actorId, NotificationType type, Long blogId) {
        notify(recipientId, actorId, type, blogId, null, null);
    }

    @Override
    @Transactional
    public void notify(Long recipientId, Long actorId, NotificationType type, Long blogId, Long commentId, Long replyId) {
        if (recipientId == null || Objects.equals(recipientId, actorId)) {
            return;
        }
        notificationRepository.save(BlogNotification.builder()
                .recipientId(recipientId)
                .actorId(actorId)
                .type(type)
                .blogId(blogId)
                .commentId(commentId)
                .replyId(replyId)
                .build());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> list(Long userId, int page, int size) {
        return SocialViews.page(notificationRepository.findByRecipientId(userId, Paging.newestFirst(page, size)),
                views::notifications);
    }

    @Override
    @Transactional(readOnly = true)
    public long unreadCount(Long userId) {
        return notificationRepository.countByRecipientIdAndReadFalse(userId);
    }

    @Override
    @Transactional
    public NotificationResponse markRead(Long notificationId, Long userId) {
        BlogNotification n = notificationRepository.findById(notificationId)
                .filter(x -> Objects.equals(x.getRecipientId(), userId))
                .orElseThrow(() -> new AppException(ErrorCode.NOTIFICATION_NOT_FOUND));
        n.setRead(true);
        return views.notifications(List.of(n)).get(0);
    }

    @Override
    @Transactional
    public int markAllRead(Long userId) {
        return notificationRepository.markAllRead(userId);
    }
}
