package com.vsl.learning.controller;

import com.vsl.common.response.ApiResponse;
import com.vsl.learning.dto.response.VocabularyResponse;
import com.vsl.learning.service.FavoriteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Favorite", description = "Từ vựng yêu thích của người dùng")
@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    @Operation(summary = "Danh sách từ yêu thích của tôi (mới nhất trước)")
    @GetMapping
    public ApiResponse<List<VocabularyResponse>> mine(@AuthenticationPrincipal Long userId) {
        return ApiResponse.ok(favoriteService.getMine(userId));
    }

    @Operation(summary = "Thêm từ vào yêu thích (gọi lại nhiều lần vẫn an toàn)")
    @PutMapping("/{vocabularyId}")
    public ApiResponse<Void> add(@AuthenticationPrincipal Long userId, @PathVariable Long vocabularyId) {
        favoriteService.add(userId, vocabularyId);
        return ApiResponse.ok("Đã thêm vào yêu thích");
    }

    @Operation(summary = "Bỏ từ khỏi yêu thích")
    @DeleteMapping("/{vocabularyId}")
    public ApiResponse<Void> remove(@AuthenticationPrincipal Long userId, @PathVariable Long vocabularyId) {
        favoriteService.remove(userId, vocabularyId);
        return ApiResponse.ok("Đã bỏ khỏi yêu thích");
    }
}
