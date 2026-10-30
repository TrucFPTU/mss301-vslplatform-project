package com.vsl.social.controller;

import com.vsl.common.response.ApiResponse;
import com.vsl.common.response.PageResponse;
import com.vsl.social.dto.BlogDetailResponse;
import com.vsl.social.dto.BlogResponse;
import com.vsl.social.dto.HideBlogRequest;
import com.vsl.social.entity.BlogStatus;
import com.vsl.social.service.BlogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Quản trị bài viết (bê từ AdminBlogController monolith). Sửa/xóa/ảnh bìa dùng chung API /api/blogs/{id}
 * (admin có quyền như tác giả). Gỡ bài phải kèm lý do; khôi phục là thao tác riêng.
 */
@Tag(name = "Blog Admin", description = "Quản trị bài viết")
@RestController
@RequestMapping("/api/blogs/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class BlogAdminController {

    private final BlogService blogService;

    @Operation(summary = "Mọi bài (trừ nháp của người khác); lọc theo trạng thái / tiêu đề")
    @GetMapping
    public ApiResponse<PageResponse<BlogResponse>> list(@AuthenticationPrincipal Long adminId,
                                                        @RequestParam(required = false) BlogStatus status,
                                                        @RequestParam(required = false) String keyword,
                                                        @RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(blogService.listForAdmin(adminId, status, keyword, page, size));
    }

    @Operation(summary = "Gỡ bài (kèm lý do hiển thị cho tác giả) - đóng mọi tố cáo đang chờ của bài")
    @PutMapping("/{id}/hide")
    public ApiResponse<BlogDetailResponse> hide(@PathVariable Long id,
                                                @AuthenticationPrincipal Long adminId,
                                                @Valid @RequestBody HideBlogRequest req) {
        return ApiResponse.ok(blogService.hide(id, adminId, req.reason()), "Đã gỡ bài viết");
    }

    @Operation(summary = "Khôi phục bài đã gỡ về công khai")
    @PutMapping("/{id}/restore")
    public ApiResponse<BlogDetailResponse> restore(@PathVariable Long id, @AuthenticationPrincipal Long adminId) {
        return ApiResponse.ok(blogService.restore(id, adminId), "Đã khôi phục bài viết");
    }
}
