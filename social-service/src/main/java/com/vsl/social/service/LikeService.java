package com.vsl.social.service;

import com.vsl.social.dto.LikeResponse;
import com.vsl.social.entity.Blog;
import com.vsl.social.entity.BlogLike;
import com.vsl.social.entity.NotificationType;
import com.vsl.social.repository.BlogLikeRepository;
import com.vsl.social.repository.BlogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Like / bỏ like. Cả hai đều idempotent: gọi lại nhiều lần không lỗi, không đếm trùng. */
@Service
@RequiredArgsConstructor
public class LikeService {

    private final BlogService blogService;
    private final BlogRepository blogRepository;
    private final BlogLikeRepository blogLikeRepository;
    private final NotificationService notificationService;

    @Transactional
    public LikeResponse like(Long blogId, Long userId) {
        Blog blog = blogService.getVisibleBlog(blogId, userId);
        if (!blogLikeRepository.existsByBlogIdAndUserId(blogId, userId)) {
            blogLikeRepository.save(BlogLike.builder().blog(blog).userId(userId).build());
            notificationService.notify(blog.getAuthorId(), userId, NotificationType.LIKE, blogId);
            blogRepository.addLikeCount(blogId, 1);
        }
        return new LikeResponse(blogId, true, blogService.getBlog(blogId).getLikeCount());
    }

    @Transactional
    public LikeResponse unlike(Long blogId, Long userId) {
        blogService.getBlog(blogId);
        blogLikeRepository.findByBlogIdAndUserId(blogId, userId).ifPresent(like -> {
            blogLikeRepository.delete(like);
            blogRepository.addLikeCount(blogId, -1);
        });
        return new LikeResponse(blogId, false, blogService.getBlog(blogId).getLikeCount());
    }
}
