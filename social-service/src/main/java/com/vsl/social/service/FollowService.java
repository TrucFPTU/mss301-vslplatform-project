package com.vsl.social.service;

import com.vsl.common.response.PageResponse;
import com.vsl.social.dto.FollowStatusResponse;
import com.vsl.social.dto.SocialUserResponse;

public interface FollowService {

    /** Theo dõi (idempotent). Người được theo dõi phải tồn tại ở identity-service. */
    FollowStatusResponse follow(Long followeeId, Long userId);

    FollowStatusResponse unfollow(Long followeeId, Long userId);

    /** Quan hệ người xem - target + số người theo dõi / đang theo dõi / bạn bè của target. */
    FollowStatusResponse status(Long targetUserId, Long viewerId);

    PageResponse<SocialUserResponse> followers(Long userId, Long viewerId, int page, int size);

    PageResponse<SocialUserResponse> following(Long userId, Long viewerId, int page, int size);

    /** Bạn bè = hai người theo dõi nhau. */
    PageResponse<SocialUserResponse> friends(Long userId, Long viewerId, int page, int size);
}
