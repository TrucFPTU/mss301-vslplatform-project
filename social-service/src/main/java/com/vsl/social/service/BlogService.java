package com.vsl.social.service;

import com.vsl.common.response.PageResponse;
import com.vsl.social.dto.BlogDetailResponse;
import com.vsl.social.dto.BlogRequest;
import com.vsl.social.dto.BlogResponse;
import com.vsl.social.dto.BlogSearchResponse;
import com.vsl.social.dto.LikerResponse;
import com.vsl.social.dto.SharedBlogResponse;
import com.vsl.social.entity.Blog;
import com.vsl.social.entity.BlogStatus;
import org.springframework.web.multipart.MultipartFile;

public interface BlogService {

    /** Tạo bài. Đăng công khai (không phải admin) phải qua chặn trùng + kiểm duyệt AI. */
    BlogDetailResponse create(Long userId, BlogRequest req);

    /** Sửa bài (tác giả / admin). Bài đã bị gỡ thì tác giả không sửa được. */
    BlogDetailResponse update(Long blogId, Long userId, BlogRequest req);

    /**
     * Xóa hẳn bài. Bị chặn nếu bài đã bị gỡ hoặc đang có tố cáo chờ xử lý (giữ căn cứ chặn đăng lại).
     * Comment/reply/like/share/report bị DB xóa theo (ON DELETE CASCADE); thông báo + ảnh bìa xóa tay.
     */
    void delete(Long blogId, Long userId);

    BlogDetailResponse getDetail(Long blogId, Long viewerId);

    /** Bảng tin công khai: người đăng nhập thấy bài của người mình theo dõi trước. keyword: tìm theo tiêu đề. */
    PageResponse<BlogResponse> listPublished(String keyword, Long viewerId, int page, int size);

    /** Tìm bài (tiêu đề / nội dung / tên tác giả) + người dùng (như monolith). */
    BlogSearchResponse search(String keyword, Long viewerId, int page, int size);

    /** Bài của một tác giả: chính tác giả / ADMIN thấy mọi trạng thái, người khác chỉ thấy PUBLISHED. */
    PageResponse<BlogResponse> listByAuthor(Long authorId, Long viewerId, int page, int size);

    /** Bài hiện trên trang cá nhân của userId nhờ được chia sẻ (chính chủ / bạn bè chia sẻ cho). */
    PageResponse<SharedBlogResponse> listSharedOnProfile(Long userId, Long viewerId, int page, int size);

    PageResponse<LikerResponse> listLikers(Long blogId, Long viewerId, int page, int size);

    /** Upload ảnh bìa (tác giả / admin) lên MinIO; xóa ảnh cũ. */
    BlogDetailResponse uploadThumbnail(Long blogId, Long userId, MultipartFile image);

    BlogDetailResponse removeThumbnail(Long blogId, Long userId);

    // ---- Quản trị ----

    /** Mọi bài trừ nháp của người khác; lọc theo trạng thái / tiêu đề. */
    PageResponse<BlogResponse> listForAdmin(Long adminId, BlogStatus status, String keyword, int page, int size);

    /** Gỡ bài: HIDDEN + lý do (hiển thị cho tác giả); đánh dấu đã xử lý mọi tố cáo đang chờ của bài. */
    BlogDetailResponse hide(Long blogId, Long adminId, String reason);

    /** Khôi phục bài đã gỡ về PUBLISHED. */
    BlogDetailResponse restore(Long blogId, Long adminId);

    // ---- Dùng chung trong social-service ----

    long countPublished();

    Blog getBlog(Long blogId);

    Blog getVisibleBlog(Long blogId, Long viewerId);

    void ensureVisible(Blog blog, Long viewerId);
}
