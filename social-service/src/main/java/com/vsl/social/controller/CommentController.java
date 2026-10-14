package com.vsl.social.controller;

import com.vsl.common.response.ApiResponse;
import com.vsl.common.response.PageResponse;
import com.vsl.social.dto.CommentRequest;
import com.vsl.social.dto.CommentResponse;
import com.vsl.social.dto.ReplyResponse;
import com.vsl.social.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Sửa/xóa bình luận và trả lời bình luận. Tạo/xem bình luận của bài nằm ở BlogController. */
@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PutMapping("/{id}")
    public ApiResponse<CommentResponse> update(@PathVariable Long id,
                                               @AuthenticationPrincipal Long userId,
                                               @Valid @RequestBody CommentRequest req) {
        return ApiResponse.ok(commentService.updateComment(id, userId, req), "Đã sửa bình luận");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, @AuthenticationPrincipal Long userId) {
        commentService.deleteComment(id, userId);
        return ApiResponse.ok("Đã xóa bình luận");
    }

    // ---- Trả lời ----

    @GetMapping("/{id}/replies")
    public ApiResponse<PageResponse<ReplyResponse>> replies(@PathVariable Long id,
                                                            @AuthenticationPrincipal Long userId,
                                                            @RequestParam(defaultValue = "0") int page,
                                                            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(commentService.listReplies(id, userId, page, size));
    }

    @PostMapping("/{id}/replies")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ReplyResponse> addReply(@PathVariable Long id,
                                               @AuthenticationPrincipal Long userId,
                                               @Valid @RequestBody CommentRequest req) {
        return ApiResponse.ok(commentService.addReply(id, userId, req), "Đã trả lời bình luận");
    }

    @PutMapping("/replies/{replyId}")
    public ApiResponse<ReplyResponse> updateReply(@PathVariable Long replyId,
                                                  @AuthenticationPrincipal Long userId,
                                                  @Valid @RequestBody CommentRequest req) {
        return ApiResponse.ok(commentService.updateReply(replyId, userId, req), "Đã sửa trả lời");
    }

    @DeleteMapping("/replies/{replyId}")
    public ApiResponse<Void> deleteReply(@PathVariable Long replyId, @AuthenticationPrincipal Long userId) {
        commentService.deleteReply(replyId, userId);
        return ApiResponse.ok("Đã xóa trả lời");
    }
}
