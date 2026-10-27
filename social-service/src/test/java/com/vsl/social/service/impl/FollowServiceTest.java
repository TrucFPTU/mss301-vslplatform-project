package com.vsl.social.service.impl;

import com.vsl.common.exception.ErrorCode;
import com.vsl.social.client.IdentityClient;
import com.vsl.social.dto.FollowStatusResponse;
import com.vsl.social.entity.NotificationType;
import com.vsl.social.entity.UserFollow;
import com.vsl.social.repository.UserFollowRepository;
import com.vsl.social.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static com.vsl.social.service.impl.TestSupport.AUTHOR;
import static com.vsl.social.service.impl.TestSupport.OTHER;
import static com.vsl.social.service.impl.TestSupport.assertAppError;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FollowServiceTest {

    @Mock
    UserFollowRepository followRepository;
    @Mock
    NotificationService notificationService;
    @Mock
    IdentityClient identityClient;
    @Mock
    SocialViews views;
    @InjectMocks
    FollowServiceImpl followService;

    @Test
    void follow_self_rejected() {
        assertAppError(() -> followService.follow(OTHER, OTHER), ErrorCode.CANNOT_FOLLOW_SELF);
        verify(followRepository, never()).save(any());
    }

    @Test
    void follow_new_savesAndNotifies() {
        when(followRepository.existsByFollowerIdAndFolloweeId(OTHER, AUTHOR)).thenReturn(false, true);
        when(identityClient.exists(AUTHOR)).thenReturn(true);

        FollowStatusResponse res = followService.follow(AUTHOR, OTHER);

        verify(followRepository).save(any(UserFollow.class));
        verify(notificationService).notify(AUTHOR, OTHER, NotificationType.FOLLOW, null);
        assertThat(res.followedByMe()).isTrue();
    }

    @Test
    void follow_unknownUser_rejected() {
        when(followRepository.existsByFollowerIdAndFolloweeId(OTHER, 404L)).thenReturn(false);
        when(identityClient.exists(404L)).thenReturn(false);

        assertAppError(() -> followService.follow(404L, OTHER), ErrorCode.USER_NOT_FOUND);
        verify(followRepository, never()).save(any());
    }

    @Test
    void follow_existing_isIdempotent() {
        when(followRepository.existsByFollowerIdAndFolloweeId(OTHER, AUTHOR)).thenReturn(true);

        followService.follow(AUTHOR, OTHER);

        verify(followRepository, never()).save(any());
        verify(notificationService, never()).notify(any(), any(), any(), any());
    }

    @Test
    void unfollow_notFollowing_isNoOp() {
        when(followRepository.findByFollowerIdAndFolloweeId(OTHER, AUTHOR)).thenReturn(Optional.empty());

        followService.unfollow(AUTHOR, OTHER);

        verify(followRepository, never()).delete(any());
    }

    @Test
    void status_guestNeverFollowing() {
        when(followRepository.countByFolloweeId(AUTHOR)).thenReturn(3L);
        when(followRepository.countByFollowerId(AUTHOR)).thenReturn(1L);
        when(followRepository.countFriends(AUTHOR)).thenReturn(1L);

        FollowStatusResponse res = followService.status(AUTHOR, null);

        assertThat(res).isEqualTo(new FollowStatusResponse(AUTHOR, false, false, false, 3, 1, 1));
        verify(followRepository, never()).existsByFollowerIdAndFolloweeId(any(), any());
    }

    @Test
    void status_mutualFollow_isFriend() {
        when(followRepository.existsByFollowerIdAndFolloweeId(OTHER, AUTHOR)).thenReturn(true);
        when(followRepository.existsByFollowerIdAndFolloweeId(AUTHOR, OTHER)).thenReturn(true);

        FollowStatusResponse res = followService.status(AUTHOR, OTHER);

        assertThat(res.friend()).isTrue();
    }
}
