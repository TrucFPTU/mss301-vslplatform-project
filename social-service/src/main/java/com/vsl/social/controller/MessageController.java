package com.vsl.social.controller;

import com.vsl.common.response.ApiResponse;
import com.vsl.social.dto.ChatThreadResponse;
import com.vsl.social.dto.ConversationResponse;
import com.vsl.social.dto.MessageRequest;
import com.vsl.social.dto.MessageResponse;
import com.vsl.social.service.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Message", description = "Tin nhắn riêng (một trong hai người theo dõi người kia)")
@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @Operation(summary = "Hộp thư: các cuộc trò chuyện, mới nhất trước")
    @GetMapping("/conversations")
    public ApiResponse<List<ConversationResponse>> conversations(@AuthenticationPrincipal Long userId) {
        return ApiResponse.ok(messageService.conversations(userId));
    }

    @Operation(summary = "Tổng số tin chưa đọc")
    @GetMapping("/unread-count")
    public ApiResponse<Long> unreadCount(@AuthenticationPrincipal Long userId) {
        return ApiResponse.ok(messageService.unreadCount(userId));
    }

    @Operation(summary = "Lịch sử trò chuyện (cũ -> mới). before = tải tin cũ hơn, after = chỉ lấy tin mới")
    @GetMapping("/{partnerId}")
    public ApiResponse<ChatThreadResponse> thread(@AuthenticationPrincipal Long userId,
                                                  @PathVariable Long partnerId,
                                                  @RequestParam(required = false) Long before,
                                                  @RequestParam(required = false) Long after,
                                                  @RequestParam(defaultValue = "30") int size) {
        return ApiResponse.ok(messageService.thread(userId, partnerId, before, after, size));
    }

    @Operation(summary = "Gửi tin nhắn")
    @PostMapping("/{partnerId}")
    public ApiResponse<MessageResponse> send(@AuthenticationPrincipal Long userId,
                                             @PathVariable Long partnerId,
                                             @Valid @RequestBody MessageRequest request) {
        return ApiResponse.ok(messageService.send(userId, partnerId, request.content()));
    }

    @Operation(summary = "Đánh dấu đã đọc các tin người này gửi cho tôi")
    @PutMapping("/{partnerId}/read")
    public ApiResponse<Integer> markRead(@AuthenticationPrincipal Long userId, @PathVariable Long partnerId) {
        return ApiResponse.ok(messageService.markRead(userId, partnerId));
    }
}
