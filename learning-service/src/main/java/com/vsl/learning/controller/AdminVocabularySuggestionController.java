package com.vsl.learning.controller;

import com.vsl.common.response.ApiResponse;
import com.vsl.common.response.PageResponse;
import com.vsl.learning.dto.response.VocabularySuggestionResponse;
import com.vsl.learning.service.VocabularySuggestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Admin Vocabulary Suggestion", description = "Admin xem và xử lý đề xuất từ vựng")
@RestController
@RequestMapping("/api/admin/vocabulary-suggestions")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminVocabularySuggestionController {

    private final VocabularySuggestionService suggestionService;

    @Operation(summary = "Danh sách tất cả đề xuất (mới nhất trước)")
    @GetMapping
    public ApiResponse<PageResponse<VocabularySuggestionResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(suggestionService.getAll(page, size));
    }

    @Operation(summary = "Số đề xuất đang chờ duyệt")
    @GetMapping("/pending-count")
    public ApiResponse<Long> pendingCount() {
        return ApiResponse.ok(suggestionService.countPending());
    }

    @Operation(summary = "Đánh dấu đề xuất đã xem")
    @PutMapping("/{id}/reviewed")
    public ApiResponse<Void> markReviewed(@PathVariable Long id) {
        suggestionService.markReviewed(id);
        return ApiResponse.ok("Đã đánh dấu đề xuất là đã xem");
    }
}
