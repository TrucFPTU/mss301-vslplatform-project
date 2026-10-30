package com.vsl.ai.controller;

import com.vsl.ai.dto.AssistantDtos.ChatRequest;
import com.vsl.ai.dto.AssistantDtos.ChatResponse;
import com.vsl.ai.service.LearningAssistantService;
import com.vsl.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Trợ lý học tập "Mentor": người học hỏi về hệ thống, tiến độ, nên học gì tiếp. */
@Tag(name = "AI - Assistant", description = "Trợ lý học tập (Gemini + dữ liệu learning-service)")
@RestController
@RequestMapping("/api/ai/assistant")
@RequiredArgsConstructor
public class AssistantController {

    private final LearningAssistantService assistantService;

    @Operation(summary = "Hỏi trợ lý học tập",
            description = "FE gửi tối đa ~10 tin gần nhất (backend không lưu lịch sử). Trả câu trả lời + thẻ từ vựng/chủ đề "
                    + "để bấm. 503 AI_3009 khi Gemini tắt/lỗi, 429 AI_3010 khi hỏi quá nhanh.")
    @PostMapping("/chat")
    public ApiResponse<ChatResponse> chat(@AuthenticationPrincipal Long userId,
                                          @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
                                          @Valid @RequestBody ChatRequest request) {
        return ApiResponse.ok(assistantService.chat(userId, authorization, request));
    }
}
