package com.vsl.social.service;

import com.vsl.common.exception.ErrorCode;
import com.vsl.social.dto.FollowStatsResponse;
import com.vsl.social.entity.NotificationType;
import com.vsl.social.entity.UserFollow;
import com.vsl.social.repository.UserFollowRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
class FollowServiceTest {

    @Mock
    UserFollowRepository followRepository;
    @Mock
    NotificationService notificationService;
    @InjectMocks
    FollowService followService;

    @Test
    void follow_self_rejected() {
        assertAppError(() -> followService.follow(OTHER, OTHER), ErrorCode.CANNOT_FOLLOW_SELF);
        verify(followRepository, never()).save(any());
    }

    @Test
    void follow_new_savesAndNotifies() {
        when(followRepository.findByFollowerIdAndFolloweeId(OTHER, AUTHOR)).thenReturn(Optional.empty());
        when(followRepository.save(any(UserFollow.class))).thenAnswer(inv -> inv.getArgument(0));

        var res = followService.follow(AUTHOR, OTHER);

        assertThat(res.followerId()).isEqualTo(OTHER);
        assertThat(res.followeeId()).isEqualTo(AUTHOR);
        verify(notificationService).notify(AUTHOR, OTHER, NotificationType.FOLLOW, null);
    }

    @Test
    void follow_existing_isIdempotent() {
        UserFollow existing = UserFollow.builder().followerId(OTHER).followeeId(AUTHOR).build();
        when(followRepository.findByFollowerIdAndFolloweeId(OTHER, AUTHOR)).thenReturn(Optional.of(existing));

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
    void stats_guestNeverFollowing() {
        when(followRepository.countByFolloweeId(AUTHOR)).thenReturn(3L);
        when(followRepository.countByFollowerId(AUTHOR)).thenReturn(1L);

        FollowStatsResponse res = followService.stats(AUTHOR, null);

        assertThat(res).isEqualTo(new FollowStatsResponse(AUTHOR, 3, 1, false));
        verify(followRepository, never()).existsByFollowerIdAndFolloweeId(any(), any());
    }
}
