package com.vsl.social.controller;

import com.vsl.common.response.ApiResponse;
import com.vsl.social.dto.CommentLikeResponse;
import com.vsl.social.service.CommentLikeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Comment Like", description = "Like / bỏ like bình luận và trả lời")
@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentLikeController {

    private final CommentLikeService commentLikeService;

    @Operation(summary = "Like một bình luận")
    @PostMapping("/{commentId}/like")
    public ApiResponse<CommentLikeResponse> likeComment(
            @PathVariable Long commentId,
            @AuthenticationPrincipal Long userId) {
        return ApiResponse.ok(commentLikeService.likeComment(commentId, userId));
    }

    @Operation(summary = "Bỏ like một bình luận")
    @DeleteMapping("/{commentId}/like")
    public ApiResponse<CommentLikeResponse> unlikeComment(
            @PathVariable Long commentId,
            @AuthenticationPrincipal Long userId) {
        return ApiResponse.ok(commentLikeService.unlikeComment(commentId, userId));
    }

    @Operation(summary = "Like một trả lời")
    @PostMapping("/replies/{replyId}/like")
    public ApiResponse<CommentLikeResponse> likeReply(
            @PathVariable Long replyId,
            @AuthenticationPrincipal Long userId) {
        return ApiResponse.ok(commentLikeService.likeReply(replyId, userId));
    }

    @Operation(summary = "Bỏ like một trả lời")
    @DeleteMapping("/replies/{replyId}/like")
    public ApiResponse<CommentLikeResponse> unlikeReply(
            @PathVariable Long replyId,
            @AuthenticationPrincipal Long userId) {
        return ApiResponse.ok(commentLikeService.unlikeReply(replyId, userId));
    }
}
