package com.vsl.social.service.impl;

import com.vsl.common.exception.AppException;
import com.vsl.common.exception.ErrorCode;
import com.vsl.common.response.PageResponse;
import com.vsl.social.client.IdentityClient;
import com.vsl.social.dto.BlockedUserResponse;
import com.vsl.social.dto.UserBriefResponse;
import com.vsl.social.dto.remote.RemoteDtos.UserSummary;
import com.vsl.social.entity.UserBlock;
import com.vsl.social.repository.UserBlockRepository;
import com.vsl.social.repository.UserFollowRepository;
import com.vsl.social.service.BlockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class BlockServiceImpl implements BlockService {

    private final UserBlockRepository blockRepository;
    private final UserFollowRepository followRepository;
    private final IdentityClient identityClient;
    private final SocialViews views;

    @Override
    @Transactional
    public void block(Long targetId, Long userId) {
        if (Objects.equals(targetId, userId)) {
            throw new AppException(ErrorCode.CANNOT_BLOCK_SELF);
        }
        if (blockRepository.existsByBlockerIdAndBlockedId(userId, targetId)) {
            return;
        }
        if (!identityClient.exists(targetId)) {
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }
        blockRepository.save(UserBlock.builder().blockerId(userId).blockedId(targetId).build());
        followRepository.findByFollowerIdAndFolloweeId(userId, targetId).ifPresent(followRepository::delete);
        followRepository.findByFollowerIdAndFolloweeId(targetId, userId).ifPresent(followRepository::delete);
    }

    @Override
    @Transactional
    public void unblock(Long targetId, Long userId) {
        blockRepository.findByBlockerIdAndBlockedId(userId, targetId).ifPresent(blockRepository::delete);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<BlockedUserResponse> listBlocked(Long userId, int page, int size) {
        return SocialViews.page(blockRepository.findByBlockerId(userId, Paging.newestFirst(page, size)), list -> {
            Map<Long, UserSummary> users = views.users(list.stream().map(UserBlock::getBlockedId).toList());
            return list.stream()
                    .map(b -> new BlockedUserResponse(UserBriefResponse.of(b.getBlockedId(), users.get(b.getBlockedId())),
                            b.getCreatedAt()))
                    .toList();
        });
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isBlockedBetween(Long a, Long b) {
        if (a == null || b == null || Objects.equals(a, b)) {
            return false;
        }
        return blockRepository.existsBetween(a, b);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasBlocked(Long userId, Long targetId) {
        return userId != null && targetId != null && blockRepository.existsByBlockerIdAndBlockedId(userId, targetId);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Long> hiddenUserIds(Long viewerId) {
        if (viewerId == null) {
            return Set.of();
        }
        return new HashSet<>(blockRepository.findRelatedUserIds(viewerId));
    }
}
