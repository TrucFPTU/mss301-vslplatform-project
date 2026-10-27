package com.vsl.ai.controller;

import com.vsl.ai.dto.ContentAiDtos.DuplicateCheckRequest;
import com.vsl.ai.dto.ContentAiDtos.DuplicateCheckResult;
import com.vsl.ai.dto.ContentAiDtos.ModerationRequest;
import com.vsl.ai.dto.ContentAiDtos.ModerationResult;
import com.vsl.ai.dto.ContentAiDtos.SynonymRequest;
import com.vsl.ai.dto.ContentAiDtos.SynonymResult;
import com.vsl.ai.service.ContentModerationService;
import com.vsl.ai.service.VideoTranscodingService;
import com.vsl.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.concurrent.CompletableFuture;

/**
 * API AI cho các service khác: kiểm duyệt nội dung, phát hiện đăng lại, tìm từ đồng nghĩa (Gemini)
 * và chuyển mã video hướng dẫn (FFmpeg). social/learning gọi qua Eureka, chuyển tiếp JWT người dùng.
 */
@Tag(name = "AI - Content", description = "Kiểm duyệt nội dung, từ đồng nghĩa, chuyển mã video")
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class ContentAiController {

    private final ContentModerationService moderationService;
    private final VideoTranscodingService transcodingService;

    @Operation(summary = "Kiểm duyệt bài viết / bình luận")
    @PostMapping("/moderation")
    public ApiResponse<ModerationResult> moderate(@Valid @RequestBody ModerationRequest request) {
        return ApiResponse.ok(moderationService.moderate(request));
    }

    @Operation(summary = "Phát hiện bài đăng lại / viết lại của bài đã có")
    @PostMapping("/moderation/duplicate")
    public ApiResponse<DuplicateCheckResult> checkDuplicate(@Valid @RequestBody DuplicateCheckRequest request) {
        return ApiResponse.ok(moderationService.checkDuplicate(request));
    }

    @Operation(summary = "Tìm từ vựng đồng nghĩa trong danh sách đã có")
    @PostMapping("/vocabulary/synonyms")
    public ApiResponse<SynonymResult> findSynonyms(@Valid @RequestBody SynonymRequest request) {
        return ApiResponse.ok(moderationService.findSynonyms(request));
    }

    @Operation(summary = "Chuyển mã video sang MP4 H.264/AAC (chỉ ADMIN - dùng khi upload video hướng dẫn)")
    @PostMapping(value = "/transcode", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public CompletableFuture<ResponseEntity<byte[]>> transcode(@RequestPart("video") MultipartFile video) {
        return transcodingService.transcodeToH264(video)
                .thenApply(bytes -> ResponseEntity.ok().contentType(MediaType.valueOf("video/mp4")).body(bytes));
    }
}
