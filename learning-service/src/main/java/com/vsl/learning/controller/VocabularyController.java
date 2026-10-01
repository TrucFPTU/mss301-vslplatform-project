package com.vsl.learning.controller;

import com.vsl.common.response.ApiResponse;
import com.vsl.common.response.PageResponse;
import com.vsl.learning.dto.request.VocabularyRequest;
import com.vsl.learning.dto.response.VocabularyResponse;
import com.vsl.learning.service.VocabularyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** Từ vựng. GET công khai; POST/PUT/DELETE chỉ ADMIN (khai trong SecurityConfig). */
@Tag(name = "Vocabulary", description = "Từ vựng ngôn ngữ ký hiệu")
@RestController
@RequestMapping("/api/vocabularies")
@RequiredArgsConstructor
public class VocabularyController {

    private final VocabularyService vocabularyService;

    @Operation(summary = "Danh sách từ vựng (phân trang)")
    @GetMapping
    public ApiResponse<PageResponse<VocabularyResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(vocabularyService.getAll(page, size));
    }

    @Operation(summary = "Chi tiết từ vựng")
    @GetMapping("/{id}")
    public ApiResponse<VocabularyResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok(vocabularyService.getById(id));
    }

    @Operation(summary = "Tìm từ vựng theo từ khoá")
    @GetMapping("/search")
    public ApiResponse<PageResponse<VocabularyResponse>> search(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(vocabularyService.search(keyword, page, size));
    }

    @Operation(summary = "Từ vựng theo danh mục")
    @GetMapping("/category/{categoryId}")
    public ApiResponse<PageResponse<VocabularyResponse>> getByCategory(
            @PathVariable Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(vocabularyService.getByCategory(categoryId, page, size));
    }

    @Operation(summary = "[ADMIN] Tạo từ vựng")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<VocabularyResponse> create(@Valid @RequestBody VocabularyRequest request) {
        return ApiResponse.ok(vocabularyService.create(request), "Tạo từ vựng thành công");
    }

    @Operation(summary = "[ADMIN] Cập nhật từ vựng")
    @PutMapping("/{id}")
    public ApiResponse<VocabularyResponse> update(@PathVariable Long id,
                                                  @Valid @RequestBody VocabularyRequest request) {
        return ApiResponse.ok(vocabularyService.update(id, request), "Cập nhật từ vựng thành công");
    }

    @Operation(summary = "[ADMIN] Xoá từ vựng")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        vocabularyService.delete(id);
        return ApiResponse.ok("Xoá từ vựng thành công");
    }
}
