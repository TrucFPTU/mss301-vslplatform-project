package com.vsl.learning.controller;

import com.vsl.common.response.ApiResponse;
import com.vsl.learning.dto.request.CreateVocabularySuggestionRequest;
import com.vsl.learning.dto.response.VocabularySuggestionResponse;
import com.vsl.learning.service.VocabularySuggestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Vocabulary Suggestion", description = "Người dùng đề xuất từ vựng mới cho admin xem")
@RestController
@RequestMapping("/api/vocabulary-suggestions")
@RequiredArgsConstructor
public class VocabularySuggestionController {

    private final VocabularySuggestionService suggestionService;

    @Operation(summary = "Gửi đề xuất từ vựng mới")
    @PostMapping
    public ApiResponse<VocabularySuggestionResponse> submit(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody CreateVocabularySuggestionRequest request) {
        return ApiResponse.ok(suggestionService.submit(userId, request), "Đã gửi đề xuất từ vựng");
    }

    @Operation(summary = "Danh sách đề xuất của tôi")
    @GetMapping("/mine")
    public ApiResponse<List<VocabularySuggestionResponse>> mine(@AuthenticationPrincipal Long userId) {
        return ApiResponse.ok(suggestionService.getMine(userId));
    }

    @Operation(summary = "Sửa đề xuất của tôi (khi còn chờ duyệt)")
    @PutMapping("/{id}")
    public ApiResponse<VocabularySuggestionResponse> update(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id,
            @Valid @RequestBody CreateVocabularySuggestionRequest request) {
        return ApiResponse.ok(suggestionService.update(userId, id, request), "Đã cập nhật đề xuất");
    }

    @Operation(summary = "Thu hồi đề xuất của tôi (khi còn chờ duyệt)")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> withdraw(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id) {
        suggestionService.withdraw(userId, id);
        return ApiResponse.ok("Đã thu hồi đề xuất");
    }
}
