package com.vsl.social.controller;

import com.vsl.common.response.ApiResponse;
import com.vsl.common.response.PageResponse;
import com.vsl.social.dto.BlogDetailResponse;
import com.vsl.social.dto.BlogRequest;
import com.vsl.social.dto.BlogResponse;
import com.vsl.social.dto.BlogSearchResponse;
import com.vsl.social.dto.CommentRequest;
import com.vsl.social.dto.CommentResponse;
import com.vsl.social.dto.LikeResponse;
import com.vsl.social.dto.LikerResponse;
import com.vsl.social.dto.PublicBlogStatsResponse;
import com.vsl.social.dto.ReportRequest;
import com.vsl.social.dto.ShareRequest;
import com.vsl.social.dto.ShareResponse;
import com.vsl.social.dto.SharedBlogResponse;
import com.vsl.social.service.BlogService;
import com.vsl.social.service.CommentService;
import com.vsl.social.service.LikeService;
import com.vsl.social.service.ReportService;
import com.vsl.social.service.ShareService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Bài viết + các tương tác trên bài (like, share, report, comment).
 * GET là public (khai trong SecurityConfig) — khi là khách, userId = null.
 */
@Tag(name = "Blog", description = "Bài viết, tìm kiếm, tương tác")
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

    @Operation(summary = "Bảng tin: bài công khai (đăng nhập thì ưu tiên người đang theo dõi); keyword lọc theo tiêu đề")
    @GetMapping
    public ApiResponse<PageResponse<BlogResponse>> list(@RequestParam(required = false) String keyword,
                                                        @AuthenticationPrincipal Long userId,
                                                        @RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(blogService.listPublished(keyword, userId, page, size));
    }

    @Operation(summary = "Tìm bài viết (tiêu đề / nội dung / tác giả) và người dùng")
    @GetMapping("/search")
    public ApiResponse<BlogSearchResponse> search(@RequestParam(required = false) String keyword,
                                                  @AuthenticationPrincipal Long userId,
                                                  @RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(blogService.search(keyword, userId, page, size));
    }

    /** Số liệu công khai cho trang chủ / dashboard tổng hợp (learning-service gọi sang). */
    @GetMapping("/public-stats")
    public ApiResponse<PublicBlogStatsResponse> publicStats() {
        return ApiResponse.ok(new PublicBlogStatsResponse(blogService.countPublished()));
    }

    @GetMapping("/{id}")
    public ApiResponse<BlogDetailResponse> detail(@PathVariable Long id,
                                                  @AuthenticationPrincipal Long userId) {
        return ApiResponse.ok(blogService.getDetail(id, userId));
    }

    @Operation(summary = "Bài của một tác giả (chính chủ / admin thấy cả nháp và bài bị gỡ)")
    @GetMapping("/user/{authorId}")
    public ApiResponse<PageResponse<BlogResponse>> byAuthor(@PathVariable Long authorId,
                                                            @AuthenticationPrincipal Long userId,
                                                            @RequestParam(defaultValue = "0") int page,
                                                            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(blogService.listByAuthor(authorId, userId, page, size));
    }

    @Operation(summary = "Bài hiện trên trang cá nhân nhờ chia sẻ (chính chủ / bạn bè chia sẻ cho)")
    @GetMapping("/user/{userId}/shared")
    public ApiResponse<PageResponse<SharedBlogResponse>> sharedOnProfile(@PathVariable Long userId,
                                                                         @AuthenticationPrincipal Long viewerId,
                                                                         @RequestParam(defaultValue = "0") int page,
                                                                         @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(blogService.listSharedOnProfile(userId, viewerId, page, size));
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

    @Operation(summary = "Tải ảnh bìa (JPEG/PNG/WEBP/GIF, tối đa 5MB)")
    @PostMapping(value = "/{id}/thumbnail", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<BlogDetailResponse> uploadThumbnail(@PathVariable Long id,
                                                           @AuthenticationPrincipal Long userId,
                                                           @RequestPart("image") MultipartFile image) {
        return ApiResponse.ok(blogService.uploadThumbnail(id, userId, image), "Đã cập nhật ảnh bìa");
    }

    @DeleteMapping("/{id}/thumbnail")
    public ApiResponse<BlogDetailResponse> removeThumbnail(@PathVariable Long id, @AuthenticationPrincipal Long userId) {
        return ApiResponse.ok(blogService.removeThumbnail(id, userId), "Đã xóa ảnh bìa");
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

    @Operation(summary = "Những người đã like bài")
    @GetMapping("/{id}/likes")
    public ApiResponse<PageResponse<LikerResponse>> likers(@PathVariable Long id,
                                                           @AuthenticationPrincipal Long userId,
                                                           @RequestParam(defaultValue = "0") int page,
                                                           @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(blogService.listLikers(id, userId, page, size));
    }

    @Operation(summary = "Chia sẻ: COPY_URL (mặc định) hoặc PROFILE (trang của mình / của một người bạn)")
    @PostMapping("/{id}/shares")
    public ApiResponse<ShareResponse> share(@PathVariable Long id,
                                            @AuthenticationPrincipal Long userId,
                                            @RequestBody(required = false) ShareRequest req) {
        return ApiResponse.ok(shareService.share(id, userId, req), "Đã chia sẻ bài viết");
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
