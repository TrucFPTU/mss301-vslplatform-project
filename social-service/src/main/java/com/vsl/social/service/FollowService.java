package com.vsl.social.service;

import com.vsl.common.exception.AppException;
import com.vsl.common.exception.ErrorCode;
import com.vsl.common.response.PageResponse;
import com.vsl.social.dto.FollowResponse;
import com.vsl.social.dto.FollowStatsResponse;
import com.vsl.social.entity.NotificationType;
import com.vsl.social.entity.UserFollow;
import com.vsl.social.repository.UserFollowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * Theo dõi người dùng. followeeId chưa được kiểm tra tồn tại (User ở identity-service) —
 * bổ sung gọi identity-service khi bên đó có API tra cứu user.
 */
@Service
@RequiredArgsConstructor
public class FollowService {

    private final UserFollowRepository followRepository;
    private final NotificationService notificationService;

    @Transactional
    public FollowResponse follow(Long followeeId, Long userId) {
        if (Objects.equals(followeeId, userId)) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Không thể tự theo dõi chính mình");
        }
        return followRepository.findByFollowerIdAndFolloweeId(userId, followeeId)
                .map(FollowResponse::from)
                .orElseGet(() -> {
                    UserFollow saved = followRepository.save(UserFollow.builder()
                            .followerId(userId)
                            .followeeId(followeeId)
                            .build());
                    notificationService.notify(followeeId, userId, NotificationType.FOLLOW, null);
                    return FollowResponse.from(saved);
                });
    }

    @Transactional
    public void unfollow(Long followeeId, Long userId) {
        followRepository.findByFollowerIdAndFolloweeId(userId, followeeId)
                .ifPresent(followRepository::delete);
    }

    @Transactional(readOnly = true)
    public PageResponse<FollowResponse> followers(Long userId, int page, int size) {
        return Paging.toResponse(
                followRepository.findByFolloweeId(userId, Paging.newestFirst(page, size)),
                FollowResponse::from);
    }

    @Transactional(readOnly = true)
    public PageResponse<FollowResponse> following(Long userId, int page, int size) {
        return Paging.toResponse(
                followRepository.findByFollowerId(userId, Paging.newestFirst(page, size)),
                FollowResponse::from);
    }

    @Transactional(readOnly = true)
    public FollowStatsResponse stats(Long userId, Long viewerId) {
        boolean followedByMe = viewerId != null && followRepository.existsByFollowerIdAndFolloweeId(viewerId, userId);
        return new FollowStatsResponse(userId,
                followRepository.countByFolloweeId(userId),
                followRepository.countByFollowerId(userId),
                followedByMe);
    }
}
