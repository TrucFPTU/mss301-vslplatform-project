package com.vsl.social.service.impl;

import com.vsl.common.exception.AppException;
import com.vsl.common.exception.ErrorCode;
import com.vsl.common.response.PageResponse;
import com.vsl.common.storage.StorageService;
import com.vsl.common.storage.UploadValidator;
import com.vsl.social.client.IdentityClient;
import com.vsl.social.dto.BlogDetailResponse;
import com.vsl.social.dto.BlogRequest;
import com.vsl.social.dto.BlogResponse;
import com.vsl.social.dto.BlogSearchResponse;
import com.vsl.social.dto.LikerResponse;
import com.vsl.social.dto.SharedBlogResponse;
import com.vsl.social.dto.SocialUserResponse;
import com.vsl.social.dto.UserBriefResponse;
import com.vsl.social.dto.remote.RemoteDtos.UserSummary;
import com.vsl.social.entity.Blog;
import com.vsl.social.entity.BlogLike;
import com.vsl.social.entity.BlogReport;
import com.vsl.social.entity.BlogShare;
import com.vsl.social.entity.BlogStatus;
import com.vsl.social.entity.ReportStatus;
import com.vsl.social.repository.BlogLikeRepository;
import com.vsl.social.repository.BlogNotificationRepository;
import com.vsl.social.repository.BlogReportRepository;
import com.vsl.social.repository.BlogRepository;
import com.vsl.social.repository.BlogShareRepository;
import com.vsl.social.service.BlogService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Nghiệp vụ bài viết (bê đủ từ BlogServiceImpl monolith).
 * Hiển thị: PUBLISHED ai cũng xem; DRAFT/HIDDEN chỉ tác giả và ADMIN (người khác nhận NOT_FOUND để không lộ bài).
 * Sửa so với monolith: admin không đặt thẳng HIDDEN/khôi phục qua "sửa bài" nữa (phải qua hide/restore có
 * lý do); chỉ kiểm duyệt lại khi tiêu đề/nội dung đổi hoặc bài chuyển sang công khai (đỡ gọi AI thừa).
 */
@Service
@RequiredArgsConstructor
public class BlogServiceImpl implements BlogService {

    static final String THUMBNAIL_BUCKET = "blog-thumbnails";

    private final BlogRepository blogRepository;
    private final BlogLikeRepository blogLikeRepository;
    private final BlogNotificationRepository notificationRepository;
    private final BlogReportRepository reportRepository;
    private final BlogShareRepository shareRepository;
    private final BlogPublishGuard publishGuard;
    private final SocialViews views;
    private final IdentityClient identityClient;
    /** Không cấu hình MinIO thì không có bean -> upload ảnh bìa báo lỗi, các chức năng khác vẫn chạy. */
    private final ObjectProvider<StorageService> storage;

    // ---------------- Tạo / sửa / xóa ----------------

    @Override
    @Transactional
    public BlogDetailResponse create(Long userId, BlogRequest req) {
        String title = req.title().trim();
        BlogStatus status = req.status() == null ? BlogStatus.PUBLISHED : requireAuthorStatus(req.status());
        if (status == BlogStatus.PUBLISHED && !AccessControl.isAdmin()) {
            publishGuard.checkBeforePublish(title, req.content(), null);
        }
        Blog blog = blogRepository.saveAndFlush(Blog.builder()
                .authorId(userId)
                .title(title)
                .content(req.content())
                .status(status)
                .build());
        return views.blogDetail(blog, userId);
    }

    @Override
    @Transactional
    public BlogDetailResponse update(Long blogId, Long userId, BlogRequest req) {
        Blog blog = getBlog(blogId);
        AccessControl.requireOwnerOrAdmin(userId, blog.getAuthorId());
        boolean admin = AccessControl.isAdmin();
        if (blog.getStatus() == BlogStatus.HIDDEN && !admin) {
            throw new AppException(ErrorCode.BLOG_HIDDEN_LOCKED);
        }

        String title = req.title().trim();
        // Bài đang bị gỡ: admin sửa được nội dung nhưng trạng thái chỉ đổi qua restore.
        BlogStatus newStatus = blog.getStatus() == BlogStatus.HIDDEN || req.status() == null
                ? blog.getStatus()
                : requireAuthorStatus(req.status());

        boolean contentChanged = !title.equals(blog.getTitle()) || !req.content().equals(blog.getContent());
        boolean becomesPublic = newStatus == BlogStatus.PUBLISHED && blog.getStatus() != BlogStatus.PUBLISHED;
        if (newStatus == BlogStatus.PUBLISHED && !admin && (contentChanged || becomesPublic)) {
            publishGuard.checkBeforePublish(title, req.content(), blogId);
        }

        blog.setTitle(title);
        blog.setContent(req.content());
        blog.setStatus(newStatus);
        return views.blogDetail(blogRepository.saveAndFlush(blog), userId);
    }

    @Override
    @Transactional
    public void delete(Long blogId, Long userId) {
        Blog blog = getBlog(blogId);
        AccessControl.requireOwnerOrAdmin(userId, blog.getAuthorId());
        // Xóa cứng sẽ cuốn theo các đơn tố cáo -> mất căn cứ chặn đăng lại. Muốn ẩn: tác giả chuyển về
        // DRAFT, admin dùng "gỡ bài" (giữ lại bài + đơn tố cáo).
        if (blog.getStatus() == BlogStatus.HIDDEN
                || reportRepository.existsByBlogIdAndStatus(blogId, ReportStatus.PENDING)) {
            throw new AppException(ErrorCode.BLOG_UNDER_REPORT);
        }
        String thumbnail = blog.getThumbnailUrl();
        notificationRepository.deleteByBlogId(blogId);
        blogRepository.delete(blog);
        deleteThumbnailQuietly(thumbnail);
    }

    // ---------------- Đọc ----------------

    @Override
    @Transactional(readOnly = true)
    public BlogDetailResponse getDetail(Long blogId, Long viewerId) {
        return views.blogDetail(getVisibleBlog(blogId, viewerId), viewerId);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<BlogResponse> listPublished(String keyword, Long viewerId, int page, int size) {
        Pageable pageable = Paging.newestFirst(page, size);
        Page<Blog> result;
        if (StringUtils.hasText(keyword)) {
            result = blogRepository.findByStatusAndTitleContainingIgnoreCase(BlogStatus.PUBLISHED, keyword.trim(), pageable);
        } else if (viewerId != null) {
            // Thứ tự đã nằm trong câu query (ưu tiên người đang theo dõi) - không thêm Sort.
            result = blogRepository.findPublishedPrioritizingFollowed(viewerId, Paging.unsorted(page, size));
        } else {
            result = blogRepository.findByStatus(BlogStatus.PUBLISHED, pageable);
        }
        return views.blogPage(result, viewerId);
    }

    @Override
    @Transactional(readOnly = true)
    public BlogSearchResponse search(String keyword, Long viewerId, int page, int size) {
        String term = keyword == null ? "" : keyword.trim();
        if (term.isEmpty()) {
            return new BlogSearchResponse(listPublished(null, viewerId, page, size),
                    PageResponse.of(List.of(), Math.max(page, 0), size, 0));
        }
        PageResponse<UserSummary> userPage = identityClient.search(term, page, Math.min(Math.max(size, 1), Paging.MAX_SIZE));
        List<Long> matchedAuthorIds = userPage.getContent().stream().map(UserSummary::userId).toList();
        Page<Blog> blogs = blogRepository.searchPublished(term,
                matchedAuthorIds.isEmpty() ? List.of(-1L) : matchedAuthorIds, Paging.newestFirst(page, size));

        Map<Long, UserSummary> known = userPage.getContent().stream()
                .collect(Collectors.toMap(UserSummary::userId, s -> s, (a, b) -> a));
        List<SocialUserResponse> users = views.socialUsers(matchedAuthorIds, known, viewerId);
        return new BlogSearchResponse(views.blogPage(blogs, viewerId),
                PageResponse.of(users, userPage.getPage(), userPage.getSize(), userPage.getTotalElements()));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<BlogResponse> listByAuthor(Long authorId, Long viewerId, int page, int size) {
        Pageable pageable = Paging.newestFirst(page, size);
        Page<Blog> result = Objects.equals(authorId, viewerId) || AccessControl.isAdmin()
                ? blogRepository.findByAuthorId(authorId, pageable)
                : blogRepository.findByAuthorIdAndStatus(authorId, BlogStatus.PUBLISHED, pageable);
        return views.blogPage(result, viewerId);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SharedBlogResponse> listSharedOnProfile(Long userId, Long viewerId, int page, int size) {
        Page<BlogShare> shares = shareRepository.findProfileShares(userId, Paging.newestFirst(page, size));
        return SocialViews.page(shares, list -> {
            Map<Long, BlogResponse> blogs = views.blogs(list.stream().map(BlogShare::getBlog).distinct().toList(), viewerId)
                    .stream().collect(Collectors.toMap(BlogResponse::id, b -> b));
            Map<Long, UserSummary> sharers = views.users(list.stream().map(BlogShare::getUserId).toList());
            List<SharedBlogResponse> out = new ArrayList<>();
            for (BlogShare s : list) {
                out.add(new SharedBlogResponse(s.getId(), UserBriefResponse.of(s.getUserId(), sharers.get(s.getUserId())),
                        s.getShareType(), s.getCreatedAt(), blogs.get(s.getBlog().getId())));
            }
            return out;
        });
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<LikerResponse> listLikers(Long blogId, Long viewerId, int page, int size) {
        getVisibleBlog(blogId, viewerId);
        Page<BlogLike> likes = blogLikeRepository.findByBlogId(blogId, Paging.newestFirst(page, size));
        return SocialViews.page(likes, list -> {
            Map<Long, UserSummary> users = views.users(list.stream().map(BlogLike::getUserId).toList());
            return list.stream()
                    .map(l -> new LikerResponse(UserBriefResponse.of(l.getUserId(), users.get(l.getUserId())), l.getCreatedAt()))
                    .toList();
        });
    }

    // ---------------- Ảnh bìa ----------------

    @Override
    @Transactional
    public BlogDetailResponse uploadThumbnail(Long blogId, Long userId, MultipartFile image) {
        Blog blog = getEditableBlog(blogId, userId);
        String ext = UploadValidator.requireImage(image);
        String objectName = UploadValidator.objectName("blog-" + blogId, ext);
        String url;
        try (InputStream in = image.getInputStream()) {
            url = storage().upload(THUMBNAIL_BUCKET, objectName, in, image.getSize(), image.getContentType());
        } catch (IOException e) {
            throw new AppException(ErrorCode.INVALID_FILE, "Không đọc được file ảnh");
        }
        String old = blog.getThumbnailUrl();
        blog.setThumbnailUrl(url);
        Blog saved = blogRepository.saveAndFlush(blog);
        deleteThumbnailQuietly(old);
        return views.blogDetail(saved, userId);
    }

    @Override
    @Transactional
    public BlogDetailResponse removeThumbnail(Long blogId, Long userId) {
        Blog blog = getEditableBlog(blogId, userId);
        String old = blog.getThumbnailUrl();
        blog.setThumbnailUrl(null);
        Blog saved = blogRepository.saveAndFlush(blog);
        deleteThumbnailQuietly(old);
        return views.blogDetail(saved, userId);
    }

    // ---------------- Quản trị ----------------

    @Override
    @Transactional(readOnly = true)
    public PageResponse<BlogResponse> listForAdmin(Long adminId, BlogStatus status, String keyword, int page, int size) {
        AccessControl.requireAdmin();
        String term = StringUtils.hasText(keyword) ? keyword.trim() : "";
        return views.blogPage(blogRepository.findForAdmin(adminId, status, term, Paging.newestFirst(page, size)), adminId);
    }

    @Override
    @Transactional
    public BlogDetailResponse hide(Long blogId, Long adminId, String reason) {
        AccessControl.requireAdmin();
        Blog blog = getBlog(blogId);
        blog.setStatus(BlogStatus.HIDDEN);
        blog.setDeletionReason(reason.trim());
        for (BlogReport report : reportRepository.findByBlogIdAndStatus(blogId, ReportStatus.PENDING)) {
            report.setStatus(ReportStatus.RESOLVED);
        }
        return views.blogDetail(blogRepository.saveAndFlush(blog), adminId);
    }

    @Override
    @Transactional
    public BlogDetailResponse restore(Long blogId, Long adminId) {
        AccessControl.requireAdmin();
        Blog blog = getBlog(blogId);
        if (blog.getStatus() != BlogStatus.HIDDEN) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Chỉ khôi phục được bài đang bị gỡ");
        }
        blog.setStatus(BlogStatus.PUBLISHED);
        blog.setDeletionReason(null);
        return views.blogDetail(blogRepository.saveAndFlush(blog), adminId);
    }

    // ---------------- Dùng chung ----------------

    @Override
    @Transactional(readOnly = true)
    public long countPublished() {
        return blogRepository.countByStatus(BlogStatus.PUBLISHED);
    }

    @Override
    public Blog getBlog(Long blogId) {
        return blogRepository.findById(blogId)
                .orElseThrow(() -> new AppException(ErrorCode.BLOG_NOT_FOUND));
    }

    @Override
    public Blog getVisibleBlog(Long blogId, Long viewerId) {
        Blog blog = getBlog(blogId);
        ensureVisible(blog, viewerId);
        return blog;
    }

    @Override
    public void ensureVisible(Blog blog, Long viewerId) {
        boolean visible = blog.getStatus() == BlogStatus.PUBLISHED
                || Objects.equals(blog.getAuthorId(), viewerId)
                || AccessControl.isAdmin();
        if (!visible) {
            throw new AppException(ErrorCode.BLOG_NOT_FOUND);
        }
    }

    /** Trạng thái người dùng được tự đặt: chỉ DRAFT / PUBLISHED. HIDDEN do admin gỡ bài. */
    private BlogStatus requireAuthorStatus(BlogStatus requested) {
        if (requested == BlogStatus.HIDDEN) {
            throw new AppException(ErrorCode.BLOG_HIDE_FORBIDDEN,
                    "Không đặt trạng thái HIDDEN khi sửa bài - admin dùng chức năng gỡ bài (kèm lý do)");
        }
        return requested;
    }

    private Blog getEditableBlog(Long blogId, Long userId) {
        Blog blog = getBlog(blogId);
        AccessControl.requireOwnerOrAdmin(userId, blog.getAuthorId());
        if (blog.getStatus() == BlogStatus.HIDDEN && !AccessControl.isAdmin()) {
            throw new AppException(ErrorCode.BLOG_HIDDEN_LOCKED);
        }
        return blog;
    }

    private StorageService storage() {
        StorageService s = storage.getIfAvailable();
        if (s == null) {
            throw new AppException(ErrorCode.FILE_UPLOAD_ERROR, "Chưa cấu hình kho lưu trữ ảnh (MinIO)");
        }
        return s;
    }

    /** Xóa ảnh trên MinIO SAU KHI transaction commit (rollback thì ảnh vẫn còn, DB không trỏ vào file đã xóa). */
    private void deleteThumbnailQuietly(String url) {
        StorageService s = storage.getIfAvailable();
        if (s == null || url == null) {
            return;
        }
        Runnable delete = () -> s.deleteByUrl(THUMBNAIL_BUCKET, url);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    delete.run();
                }
            });
        } else {
            delete.run();
        }
    }
}
