package com.vsl.social.controller;

import com.vsl.common.response.ApiResponse;
import com.vsl.common.response.PageResponse;
import com.vsl.social.dto.FollowStatusResponse;
import com.vsl.social.dto.SocialUserResponse;
import com.vsl.social.service.FollowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Theo dõi người dùng. GET là public; người xem (nếu đăng nhập) nhận thêm cờ quan hệ. */
@Tag(name = "Follow", description = "Theo dõi, người theo dõi, bạn bè")
@RestController
@RequestMapping("/api/follows")
@RequiredArgsConstructor
public class FollowController {

    private final FollowService followService;

    @PostMapping("/{userId}")
    public ApiResponse<FollowStatusResponse> follow(@PathVariable Long userId,
                                                    @AuthenticationPrincipal Long currentUserId) {
        return ApiResponse.ok(followService.follow(userId, currentUserId), "Đã theo dõi");
    }

    @DeleteMapping("/{userId}")
    public ApiResponse<FollowStatusResponse> unfollow(@PathVariable Long userId,
                                                      @AuthenticationPrincipal Long currentUserId) {
        return ApiResponse.ok(followService.unfollow(userId, currentUserId), "Đã bỏ theo dõi");
    }

    @Operation(summary = "Quan hệ với người xem + số người theo dõi / đang theo dõi / bạn bè")
    @GetMapping("/{userId}/stats")
    public ApiResponse<FollowStatusResponse> stats(@PathVariable Long userId,
                                                   @AuthenticationPrincipal Long currentUserId) {
        return ApiResponse.ok(followService.status(userId, currentUserId));
    }

    @GetMapping("/{userId}/followers")
    public ApiResponse<PageResponse<SocialUserResponse>> followers(@PathVariable Long userId,
                                                                   @AuthenticationPrincipal Long currentUserId,
                                                                   @RequestParam(defaultValue = "0") int page,
                                                                   @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(followService.followers(userId, currentUserId, page, size));
    }

    @GetMapping("/{userId}/following")
    public ApiResponse<PageResponse<SocialUserResponse>> following(@PathVariable Long userId,
                                                                   @AuthenticationPrincipal Long currentUserId,
                                                                   @RequestParam(defaultValue = "0") int page,
                                                                   @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(followService.following(userId, currentUserId, page, size));
    }

    @Operation(summary = "Bạn bè (hai người theo dõi nhau)")
    @GetMapping("/{userId}/friends")
    public ApiResponse<PageResponse<SocialUserResponse>> friends(@PathVariable Long userId,
                                                                 @AuthenticationPrincipal Long currentUserId,
                                                                 @RequestParam(defaultValue = "0") int page,
                                                                 @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(followService.friends(userId, currentUserId, page, size));
    }
}
