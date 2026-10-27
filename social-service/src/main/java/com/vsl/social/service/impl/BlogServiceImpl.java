package com.vsl.social.service.impl;

import com.vsl.common.exception.AppException;
import com.vsl.common.exception.ErrorCode;
import com.vsl.common.response.PageResponse;
import com.vsl.social.dto.BlogDetailResponse;
import com.vsl.social.dto.BlogRequest;
import com.vsl.social.dto.BlogResponse;
import com.vsl.social.entity.Blog;
import com.vsl.social.entity.BlogStatus;
import com.vsl.social.repository.BlogLikeRepository;
import com.vsl.social.repository.BlogNotificationRepository;
import com.vsl.social.repository.BlogRepository;
import com.vsl.social.service.BlogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Objects;

/**
 * Nghiệp vụ bài viết.
 * Quy tắc hiển thị: PUBLISHED ai cũng xem được; DRAFT/HIDDEN chỉ tác giả và ADMIN thấy
 * (người khác nhận NOT_FOUND để không lộ bài tồn tại).
 */
@Service
@RequiredArgsConstructor
public class BlogServiceImpl implements BlogService {

    private final BlogRepository blogRepository;
    private final BlogLikeRepository blogLikeRepository;
    private final BlogNotificationRepository notificationRepository;

    @Override
    @Transactional
    public BlogDetailResponse create(Long userId, BlogRequest req) {
        Blog blog = Blog.builder()
                .authorId(userId)
                .title(req.title().trim())
                .content(req.content())
                .thumbnailUrl(req.thumbnailUrl())
                .status(resolveRequestedStatus(req.status()))
                .build();
        return BlogDetailResponse.from(blogRepository.saveAndFlush(blog), false);
    }

    @Override
    @Transactional
    public BlogDetailResponse update(Long blogId, Long userId, BlogRequest req) {
        Blog blog = getBlog(blogId);
        AccessControl.requireOwnerOrAdmin(userId, blog.getAuthorId());

        blog.setTitle(req.title().trim());
        blog.setContent(req.content());
        blog.setThumbnailUrl(req.thumbnailUrl());
        // Bài đã bị admin ẩn thì tác giả không tự mở lại được.
        if (blog.getStatus() != BlogStatus.HIDDEN || AccessControl.isAdmin()) {
            blog.setStatus(resolveRequestedStatus(req.status()));
        }
        Blog saved = blogRepository.saveAndFlush(blog);
        return BlogDetailResponse.from(saved, blogLikeRepository.existsByBlogIdAndUserId(blogId, userId));
    }

    @Override
    @Transactional
    public void delete(Long blogId, Long userId) {
        Blog blog = getBlog(blogId);
        AccessControl.requireOwnerOrAdmin(userId, blog.getAuthorId());
        notificationRepository.deleteByBlogId(blogId);
        blogRepository.delete(blog);
    }

    @Override
    @Transactional(readOnly = true)
    public BlogDetailResponse getDetail(Long blogId, Long viewerId) {
        Blog blog = getVisibleBlog(blogId, viewerId);
        boolean liked = viewerId != null && blogLikeRepository.existsByBlogIdAndUserId(blogId, viewerId);
        return BlogDetailResponse.from(blog, liked);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<BlogResponse> listPublished(String keyword, int page, int size) {
        Pageable pageable = Paging.newestFirst(page, size);
        Page<Blog> result = StringUtils.hasText(keyword)
                ? blogRepository.findByStatusAndTitleContainingIgnoreCase(BlogStatus.PUBLISHED, keyword.trim(), pageable)
                : blogRepository.findByStatus(BlogStatus.PUBLISHED, pageable);
        return Paging.toResponse(result, BlogResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<BlogResponse> listByAuthor(Long authorId, Long viewerId, int page, int size) {
        Pageable pageable = Paging.newestFirst(page, size);
        Page<Blog> result = Objects.equals(authorId, viewerId) || AccessControl.isAdmin()
                ? blogRepository.findByAuthorId(authorId, pageable)
                : blogRepository.findByAuthorIdAndStatus(authorId, BlogStatus.PUBLISHED, pageable);
        return Paging.toResponse(result, BlogResponse::from);
    }

    // ---- Dùng chung cho các service khác trong social-service ----

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

    private BlogStatus resolveRequestedStatus(BlogStatus requested) {
        if (requested == null) {
            return BlogStatus.PUBLISHED;
        }
        if (requested == BlogStatus.HIDDEN && !AccessControl.isAdmin()) {
            throw new AppException(ErrorCode.BLOG_HIDE_FORBIDDEN);
        }
        return requested;
    }
}
