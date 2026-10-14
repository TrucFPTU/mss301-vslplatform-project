package com.vsl.social.controller;

import com.vsl.common.response.ApiResponse;
import com.vsl.common.response.PageResponse;
import com.vsl.social.dto.BlogDetailResponse;
import com.vsl.social.dto.BlogRequest;
import com.vsl.social.dto.BlogResponse;
import com.vsl.social.dto.CommentRequest;
import com.vsl.social.dto.CommentResponse;
import com.vsl.social.dto.LikeResponse;
import com.vsl.social.dto.ReportRequest;
import com.vsl.social.service.BlogService;
import com.vsl.social.service.CommentService;
import com.vsl.social.service.LikeService;
import com.vsl.social.service.ReportService;
import com.vsl.social.service.ShareService;
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

/**
 * Bài viết + các tương tác trên bài (like, share, report, comment).
 * GET là public (khai trong SecurityConfig) — khi là khách, userId = null.
 */
@RestController
@RequestMapping("/api/blogs")
@RequiredArgsConstructor
public class BlogController {

    private final BlogService blogService;
    private final LikeService likeService;
    private final ShareService shareService;
    private final ReportService reportService;
    private final CommentService commentService;

    // ---- Bài viết ----

    @GetMapping
    public ApiResponse<PageResponse<BlogResponse>> list(@RequestParam(required = false) String keyword,
                                                        @RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(blogService.listPublished(keyword, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<BlogDetailResponse> detail(@PathVariable Long id,
                                                  @AuthenticationPrincipal Long userId) {
        return ApiResponse.ok(blogService.getDetail(id, userId));
    }

    @GetMapping("/user/{authorId}")
    public ApiResponse<PageResponse<BlogResponse>> byAuthor(@PathVariable Long authorId,
                                                            @AuthenticationPrincipal Long userId,
                                                            @RequestParam(defaultValue = "0") int page,
                                                            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(blogService.listByAuthor(authorId, userId, page, size));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<BlogDetailResponse> create(@AuthenticationPrincipal Long userId,
                                                  @Valid @RequestBody BlogRequest req) {
        return ApiResponse.ok(blogService.create(userId, req), "Đã tạo bài viết");
    }

    @PutMapping("/{id}")
    public ApiResponse<BlogDetailResponse> update(@PathVariable Long id,
                                                  @AuthenticationPrincipal Long userId,
                                                  @Valid @RequestBody BlogRequest req) {
        return ApiResponse.ok(blogService.update(id, userId, req), "Đã cập nhật bài viết");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, @AuthenticationPrincipal Long userId) {
        blogService.delete(id, userId);
        return ApiResponse.ok("Đã xóa bài viết");
    }

    // ---- Like / Share / Report ----

    @PostMapping("/{id}/likes")
    public ApiResponse<LikeResponse> like(@PathVariable Long id, @AuthenticationPrincipal Long userId) {
        return ApiResponse.ok(likeService.like(id, userId));
    }

    @DeleteMapping("/{id}/likes")
    public ApiResponse<LikeResponse> unlike(@PathVariable Long id, @AuthenticationPrincipal Long userId) {
        return ApiResponse.ok(likeService.unlike(id, userId));
    }

    /** Trả về tổng số lượt chia sẻ sau khi chia sẻ. */
    @PostMapping("/{id}/shares")
    public ApiResponse<Long> share(@PathVariable Long id, @AuthenticationPrincipal Long userId) {
        return ApiResponse.ok(shareService.share(id, userId), "Đã chia sẻ bài viết");
    }

    @PostMapping("/{id}/reports")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Void> report(@PathVariable Long id,
                                    @AuthenticationPrincipal Long userId,
                                    @Valid @RequestBody ReportRequest req) {
        reportService.report(id, userId, req);
        return ApiResponse.ok("Đã gửi báo cáo");
    }

    // ---- Bình luận của bài ----

    @GetMapping("/{id}/comments")
    public ApiResponse<PageResponse<CommentResponse>> comments(@PathVariable Long id,
                                                               @AuthenticationPrincipal Long userId,
                                                               @RequestParam(defaultValue = "0") int page,
                                                               @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(commentService.listComments(id, userId, page, size));
    }

    @PostMapping("/{id}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CommentResponse> addComment(@PathVariable Long id,
                                                   @AuthenticationPrincipal Long userId,
                                                   @Valid @RequestBody CommentRequest req) {
        return ApiResponse.ok(commentService.addComment(id, userId, req), "Đã bình luận");
    }
}
