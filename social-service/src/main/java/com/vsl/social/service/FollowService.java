package com.vsl.social.service;

import com.vsl.common.response.PageResponse;
import com.vsl.social.dto.FollowResponse;
import com.vsl.social.dto.FollowStatsResponse;

public interface FollowService {

    FollowResponse follow(Long followeeId, Long userId);

    void unfollow(Long followeeId, Long userId);

    PageResponse<FollowResponse> followers(Long userId, int page, int size);

    PageResponse<FollowResponse> following(Long userId, int page, int size);

    FollowStatsResponse stats(Long userId, Long viewerId);
}
