package com.vsl.social.service;

import com.vsl.social.entity.Blog;
import com.vsl.social.entity.BlogShare;
import com.vsl.social.entity.NotificationType;
import com.vsl.social.repository.BlogRepository;
import com.vsl.social.repository.BlogShareRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ShareService {

    private final BlogService blogService;
    private final BlogRepository blogRepository;
    private final BlogShareRepository blogShareRepository;
    private final NotificationService notificationService;

    /** Ghi nhận một lượt chia sẻ, trả về tổng số lượt chia sẻ mới. */
    @Transactional
    public long share(Long blogId, Long userId) {
        Blog blog = blogService.getVisibleBlog(blogId, userId);
        blogShareRepository.save(BlogShare.builder().blog(blog).userId(userId).build());
        notificationService.notify(blog.getAuthorId(), userId, NotificationType.SHARE, blogId);
        blogRepository.addShareCount(blogId, 1);
        return blogService.getBlog(blogId).getShareCount();
    }
}
