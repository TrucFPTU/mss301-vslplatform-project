package com.vsl.social.service.impl;

import com.vsl.common.exception.AppException;
import com.vsl.common.exception.ErrorCode;
import com.vsl.common.response.PageResponse;
import com.vsl.social.client.IdentityClient;
import com.vsl.social.dto.FollowStatusResponse;
import com.vsl.social.dto.SocialUserResponse;
import com.vsl.social.entity.NotificationType;
import com.vsl.social.entity.UserFollow;
import com.vsl.social.repository.UserFollowRepository;
import com.vsl.social.service.FollowService;
import com.vsl.social.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Objects;

/**
 * Theo dõi người dùng (bê từ FollowServiceImpl monolith: trạng thái, bạn bè, danh sách kèm tên + quan hệ).
 * Người được theo dõi được kiểm tra tồn tại qua identity-service.
 */
@Service
@RequiredArgsConstructor
public class FollowServiceImpl implements FollowService {

    private final UserFollowRepository followRepository;
    private final NotificationService notificationService;
    private final IdentityClient identityClient;
    private final SocialViews views;

    @Override
    @Transactional
    public FollowStatusResponse follow(Long followeeId, Long userId) {
        if (Objects.equals(followeeId, userId)) {
            throw new AppException(ErrorCode.CANNOT_FOLLOW_SELF);
        }
        if (!followRepository.existsByFollowerIdAndFolloweeId(userId, followeeId)) {
            if (!identityClient.exists(followeeId)) {
                throw new AppException(ErrorCode.USER_NOT_FOUND);
            }
            followRepository.save(UserFollow.builder().followerId(userId).followeeId(followeeId).build());
            notificationService.notify(followeeId, userId, NotificationType.FOLLOW, null);
        }
        return status(followeeId, userId);
    }

    @Override
    @Transactional
    public FollowStatusResponse unfollow(Long followeeId, Long userId) {
        followRepository.findByFollowerIdAndFolloweeId(userId, followeeId).ifPresent(followRepository::delete);
        return status(followeeId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public FollowStatusResponse status(Long targetUserId, Long viewerId) {
        boolean self = Objects.equals(targetUserId, viewerId);
        boolean followedByMe = viewerId != null && !self
                && followRepository.existsByFollowerIdAndFolloweeId(viewerId, targetUserId);
        boolean followsMe = viewerId != null && !self
                && followRepository.existsByFollowerIdAndFolloweeId(targetUserId, viewerId);
        return new FollowStatusResponse(targetUserId, followedByMe, followsMe, followedByMe && followsMe,
                followRepository.countByFolloweeId(targetUserId),
                followRepository.countByFollowerId(targetUserId),
                followRepository.countFriends(targetUserId));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SocialUserResponse> followers(Long userId, Long viewerId, int page, int size) {
        return SocialViews.page(followRepository.findByFolloweeId(userId, Paging.newestFirst(page, size)),
                list -> views.socialUsers(list.stream().map(UserFollow::getFollowerId).toList(), Map.of(), viewerId));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SocialUserResponse> following(Long userId, Long viewerId, int page, int size) {
        return SocialViews.page(followRepository.findByFollowerId(userId, Paging.newestFirst(page, size)),
                list -> views.socialUsers(list.stream().map(UserFollow::getFolloweeId).toList(), Map.of(), viewerId));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SocialUserResponse> friends(Long userId, Long viewerId, int page, int size) {
        return SocialViews.page(followRepository.findFriends(userId, Paging.newestFirst(page, size)),
                list -> views.socialUsers(list.stream().map(UserFollow::getFollowerId).toList(), Map.of(), viewerId));
    }
}
