package com.vsl.social.service.impl;

import com.vsl.common.exception.ErrorCode;
import com.vsl.social.dto.NotificationResponse;
import com.vsl.social.entity.BlogNotification;
import com.vsl.social.entity.NotificationType;
import com.vsl.social.repository.BlogNotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static com.vsl.social.service.impl.TestSupport.AUTHOR;
import static com.vsl.social.service.impl.TestSupport.OTHER;
import static com.vsl.social.service.impl.TestSupport.assertAppError;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    BlogNotificationRepository notificationRepository;
    @Mock
    SocialViews views;
    @InjectMocks
    NotificationServiceImpl notificationService;

    @Test
    void notify_self_isSkipped() {
        notificationService.notify(AUTHOR, AUTHOR, NotificationType.LIKE, 1L);
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void notify_nullRecipient_isSkipped() {
        notificationService.notify(null, OTHER, NotificationType.LIKE, 1L);
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void notify_other_savesUnreadWithCommentAndReply() {
        notificationService.notify(AUTHOR, OTHER, NotificationType.MENTION, 1L, 10L, 20L);

        ArgumentCaptor<BlogNotification> saved = ArgumentCaptor.forClass(BlogNotification.class);
        verify(notificationRepository).save(saved.capture());
        BlogNotification n = saved.getValue();
        assertThat(n.getRecipientId()).isEqualTo(AUTHOR);
        assertThat(n.getActorId()).isEqualTo(OTHER);
        assertThat(n.getType()).isEqualTo(NotificationType.MENTION);
        assertThat(n.getBlogId()).isEqualTo(1L);
        assertThat(n.getCommentId()).isEqualTo(10L);
        assertThat(n.getReplyId()).isEqualTo(20L);
        assertThat(n.isRead()).isFalse();
    }

    @Test
    void markRead_someoneElsesNotification_notFound() {
        BlogNotification n = BlogNotification.builder().recipientId(AUTHOR).actorId(OTHER).type(NotificationType.LIKE).build();
        when(notificationRepository.findById(5L)).thenReturn(Optional.of(n));

        assertAppError(() -> notificationService.markRead(5L, OTHER), ErrorCode.NOTIFICATION_NOT_FOUND);
        assertThat(n.isRead()).isFalse();
    }

    @Test
    void markRead_own_setsRead() {
        BlogNotification n = BlogNotification.builder().recipientId(AUTHOR).actorId(OTHER).type(NotificationType.LIKE).build();
        when(notificationRepository.findById(5L)).thenReturn(Optional.of(n));
        when(views.notifications(List.of(n))).thenReturn(List.of(mock(NotificationResponse.class)));

        notificationService.markRead(5L, AUTHOR);

        assertThat(n.isRead()).isTrue();
    }
}
