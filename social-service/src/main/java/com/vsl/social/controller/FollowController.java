package com.vsl.social.controller;

import com.vsl.common.response.ApiResponse;
import com.vsl.common.response.PageResponse;
import com.vsl.social.dto.FollowResponse;
import com.vsl.social.dto.FollowStatsResponse;
import com.vsl.social.service.FollowService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** {userId} trong đường dẫn là người được theo dõi / người cần xem danh sách. */
@RestController
@RequestMapping("/api/follows")
@RequiredArgsConstructor
public class FollowController {

    private final FollowService followService;

    @PostMapping("/{userId}")
    public ApiResponse<FollowResponse> follow(@PathVariable Long userId,
                                              @AuthenticationPrincipal Long currentUserId) {
        return ApiResponse.ok(followService.follow(userId, currentUserId), "Đã theo dõi");
    }

    @DeleteMapping("/{userId}")
    public ApiResponse<Void> unfollow(@PathVariable Long userId,
                                      @AuthenticationPrincipal Long currentUserId) {
        followService.unfollow(userId, currentUserId);
        return ApiResponse.ok("Đã bỏ theo dõi");
    }

    @GetMapping("/{userId}/followers")
    public ApiResponse<PageResponse<FollowResponse>> followers(@PathVariable Long userId,
                                                               @RequestParam(defaultValue = "0") int page,
                                                               @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(followService.followers(userId, page, size));
    }

    @GetMapping("/{userId}/following")
    public ApiResponse<PageResponse<FollowResponse>> following(@PathVariable Long userId,
                                                               @RequestParam(defaultValue = "0") int page,
                                                               @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(followService.following(userId, page, size));
    }

    @GetMapping("/{userId}/stats")
    public ApiResponse<FollowStatsResponse> stats(@PathVariable Long userId,
                                                  @AuthenticationPrincipal Long currentUserId) {
        return ApiResponse.ok(followService.stats(userId, currentUserId));
    }
}
