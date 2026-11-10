package com.vsl.social.service.impl;

import com.vsl.social.dto.UserCleanupResponse;
import com.vsl.social.entity.BlogStatus;
import com.vsl.social.repository.BlogNotificationRepository;
import com.vsl.social.repository.BlogRepository;
import com.vsl.social.repository.UserBlockRepository;
import com.vsl.social.repository.UserFollowRepository;
import com.vsl.social.service.UserCleanupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserCleanupServiceTest {

    @Mock
    UserFollowRepository followRepository;
    @Mock
    UserBlockRepository blockRepository;
    @Mock
    BlogNotificationRepository notificationRepository;
    @Mock
    BlogRepository blogRepository;

    UserCleanupServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UserCleanupServiceImpl(followRepository, blockRepository, notificationRepository, blogRepository);
    }

    @Test
    void cleanup_removesFollowsBlocksNotifications_andHidesPosts() {
        when(followRepository.deleteAllInvolving(7L)).thenReturn(3);
        when(blockRepository.deleteAllInvolving(7L)).thenReturn(1);
        when(notificationRepository.deleteByRecipient(7L)).thenReturn(5);
        when(blogRepository.hideAllByAuthor(7L, BlogStatus.HIDDEN, UserCleanupService.AUTHOR_DELETED_REASON)).thenReturn(2);

        UserCleanupResponse res = service.cleanupDeletedUser(7L);

        assertThat(res).isEqualTo(new UserCleanupResponse(3, 1, 5, 2));
        verify(blogRepository).hideAllByAuthor(7L, BlogStatus.HIDDEN, "Tác giả đã xóa tài khoản");
    }

    @Test
    void cleanup_calledAgain_returnsZeros() {
        // Lần 2 (relay gửi lại / at-least-once): không còn gì để dọn, không lỗi.
        UserCleanupResponse res = service.cleanupDeletedUser(7L);

        assertThat(res).isEqualTo(new UserCleanupResponse(0, 0, 0, 0));
    }
}
