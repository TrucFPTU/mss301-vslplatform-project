package com.vsl.social.service.impl;

import com.vsl.common.exception.ErrorCode;
import com.vsl.social.client.IdentityClient;
import com.vsl.social.entity.UserBlock;
import com.vsl.social.entity.UserFollow;
import com.vsl.social.repository.UserBlockRepository;
import com.vsl.social.repository.UserFollowRepository;
import com.vsl.social.service.BlockService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.vsl.social.service.impl.TestSupport.AUTHOR;
import static com.vsl.social.service.impl.TestSupport.OTHER;
import static com.vsl.social.service.impl.TestSupport.assertAppError;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BlockServiceTest {

    @Mock
    UserBlockRepository blockRepository;
    @Mock
    UserFollowRepository followRepository;
    @Mock
    IdentityClient identityClient;
    @Mock
    SocialViews views;
    @InjectMocks
    BlockServiceImpl blockService;

    @Test
    void block_self_rejected() {
        assertAppError(() -> blockService.block(OTHER, OTHER), ErrorCode.CANNOT_BLOCK_SELF);
        verify(blockRepository, never()).save(any());
    }

    @Test
    void block_unknownUser_rejected() {
        when(identityClient.exists(AUTHOR)).thenReturn(false);

        assertAppError(() -> blockService.block(AUTHOR, OTHER), ErrorCode.USER_NOT_FOUND);
        verify(blockRepository, never()).save(any());
    }

    @Test
    void block_savesAndRemovesFollowsBothWays() {
        UserFollow mine = UserFollow.builder().followerId(OTHER).followeeId(AUTHOR).build();
        UserFollow theirs = UserFollow.builder().followerId(AUTHOR).followeeId(OTHER).build();
        when(identityClient.exists(AUTHOR)).thenReturn(true);
        when(followRepository.findByFollowerIdAndFolloweeId(OTHER, AUTHOR)).thenReturn(Optional.of(mine));
        when(followRepository.findByFollowerIdAndFolloweeId(AUTHOR, OTHER)).thenReturn(Optional.of(theirs));

        blockService.block(AUTHOR, OTHER);

        verify(blockRepository).save(any(UserBlock.class));
        verify(followRepository).delete(mine);
        verify(followRepository).delete(theirs);
    }

    @Test
    void block_twice_isIdempotent() {
        when(blockRepository.existsByBlockerIdAndBlockedId(OTHER, AUTHOR)).thenReturn(true);

        blockService.block(AUTHOR, OTHER);

        verify(blockRepository, never()).save(any());
        verify(identityClient, never()).exists(any());
    }

    @Test
    void unblock_onlyRemovesMyOwnBlock() {
        UserBlock b = UserBlock.builder().blockerId(OTHER).blockedId(AUTHOR).build();
        when(blockRepository.findByBlockerIdAndBlockedId(OTHER, AUTHOR)).thenReturn(Optional.of(b));

        blockService.unblock(AUTHOR, OTHER);

        verify(blockRepository).delete(b);
    }

    @Test
    void unblock_whenBlockedByOther_doesNothing() {
        when(blockRepository.findByBlockerIdAndBlockedId(OTHER, AUTHOR)).thenReturn(Optional.empty());

        blockService.unblock(AUTHOR, OTHER);

        verify(blockRepository, never()).delete(any());
    }

    @Test
    void isBlockedBetween_guestOrSelf_false() {
        assertThat(blockService.isBlockedBetween(AUTHOR, null)).isFalse();
        assertThat(blockService.isBlockedBetween(null, AUTHOR)).isFalse();
        assertThat(blockService.isBlockedBetween(AUTHOR, AUTHOR)).isFalse();
        verify(blockRepository, never()).existsBetween(any(), any());
    }

    @Test
    void hiddenUserIds_guestEmpty_userBothDirections() {
        when(blockRepository.findRelatedUserIds(OTHER)).thenReturn(List.of(AUTHOR, 7L));

        assertThat(blockService.hiddenUserIds(null)).isEmpty();
        assertThat(blockService.hiddenUserIds(OTHER)).containsExactlyInAnyOrder(AUTHOR, 7L);
    }

    @Test
    void forNotIn_neverEmpty() {
        assertThat(BlockService.forNotIn(Set.of())).containsExactly(-1L);
        assertThat(BlockService.forNotIn(Set.of(5L))).containsExactly(5L);
    }
}
