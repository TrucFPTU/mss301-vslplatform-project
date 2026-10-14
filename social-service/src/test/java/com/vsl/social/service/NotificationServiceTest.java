package com.vsl.social.service;

import com.vsl.common.exception.ErrorCode;
import com.vsl.social.entity.BlogNotification;
import com.vsl.social.entity.NotificationType;
import com.vsl.social.repository.BlogNotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static com.vsl.social.service.TestSupport.AUTHOR;
import static com.vsl.social.service.TestSupport.OTHER;
import static com.vsl.social.service.TestSupport.assertAppError;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    BlogNotificationRepository notificationRepository;
    @InjectMocks
    NotificationService notificationService;

    @Test
    void notify_self_isSkipped() {
        notificationService.notify(AUTHOR, AUTHOR, NotificationType.LIKE, 1L);
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void notify_other_savesUnread() {
        notificationService.notify(AUTHOR, OTHER, NotificationType.COMMENT, 1L);

        ArgumentCaptor<BlogNotification> saved = ArgumentCaptor.forClass(BlogNotification.class);
        verify(notificationRepository).save(saved.capture());
        BlogNotification n = saved.getValue();
        assertThat(n.getRecipientId()).isEqualTo(AUTHOR);
        assertThat(n.getActorId()).isEqualTo(OTHER);
        assertThat(n.getType()).isEqualTo(NotificationType.COMMENT);
        assertThat(n.getBlogId()).isEqualTo(1L);
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

        assertThat(notificationService.markRead(5L, AUTHOR).read()).isTrue();
    }
}
