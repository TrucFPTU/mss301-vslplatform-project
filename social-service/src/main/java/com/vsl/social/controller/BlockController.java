package com.vsl.social.controller;

import com.vsl.common.response.ApiResponse;
import com.vsl.common.response.PageResponse;
import com.vsl.social.dto.BlockedUserResponse;
import com.vsl.social.service.BlockService;
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

/** Chặn / mở chặn người dùng. Mọi tuyến đều cần đăng nhập. */
@Tag(name = "Block", description = "Chặn người dùng (ẩn bài viết, chặn nhắn tin / theo dõi hai chiều)")
@RestController
@RequestMapping("/api/blocks")
@RequiredArgsConstructor
public class BlockController {

    private final BlockService blockService;

    @Operation(summary = "Những người tôi đã chặn, mới nhất trước")
    @GetMapping
    public ApiResponse<PageResponse<BlockedUserResponse>> list(@AuthenticationPrincipal Long userId,
                                                               @RequestParam(defaultValue = "0") int page,
                                                               @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(blockService.listBlocked(userId, page, size));
    }

    @Operation(summary = "Chặn (xóa luôn quan hệ theo dõi giữa hai người)")
    @PostMapping("/{userId}")
    public ApiResponse<Void> block(@PathVariable Long userId, @AuthenticationPrincipal Long currentUserId) {
        blockService.block(userId, currentUserId);
        return ApiResponse.ok("Đã chặn người dùng");
    }

    @DeleteMapping("/{userId}")
    public ApiResponse<Void> unblock(@PathVariable Long userId, @AuthenticationPrincipal Long currentUserId) {
        blockService.unblock(userId, currentUserId);
        return ApiResponse.ok("Đã bỏ chặn người dùng");
    }
}
