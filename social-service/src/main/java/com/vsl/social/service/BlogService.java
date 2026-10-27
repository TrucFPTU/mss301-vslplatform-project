package com.vsl.social.service;

import com.vsl.common.response.PageResponse;
import com.vsl.social.dto.BlogDetailResponse;
import com.vsl.social.dto.BlogRequest;
import com.vsl.social.dto.BlogResponse;
import com.vsl.social.entity.Blog;

public interface BlogService {

    BlogDetailResponse create(Long userId, BlogRequest req);

    BlogDetailResponse update(Long blogId, Long userId, BlogRequest req);

    /** Xóa bài: comment/reply/like/share/report bị DB xóa theo (ON DELETE CASCADE); thông báo xóa tay. */
    void delete(Long blogId, Long userId);

    BlogDetailResponse getDetail(Long blogId, Long viewerId);

    /** Danh sách bài công khai, mới nhất trước; keyword tìm theo tiêu đề. */
    PageResponse<BlogResponse> listPublished(String keyword, int page, int size);

    /** Bài của một tác giả: chính tác giả / ADMIN thấy mọi trạng thái, người khác chỉ thấy PUBLISHED. */
    PageResponse<BlogResponse> listByAuthor(Long authorId, Long viewerId, int page, int size);

    Blog getBlog(Long blogId);

    Blog getVisibleBlog(Long blogId, Long viewerId);

    void ensureVisible(Blog blog, Long viewerId);

    /** Số bài đã xuất bản (PUBLISHED), không tính nháp / đã gỡ. */
    long countPublished();
}
