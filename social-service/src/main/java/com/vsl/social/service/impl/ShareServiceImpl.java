package com.vsl.social.service.impl;

import com.vsl.social.entity.Blog;
import com.vsl.social.entity.BlogShare;
import com.vsl.social.entity.NotificationType;
import com.vsl.social.repository.BlogRepository;
import com.vsl.social.repository.BlogShareRepository;
import com.vsl.social.service.BlogService;
import com.vsl.social.service.NotificationService;
import com.vsl.social.service.ShareService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ShareServiceImpl implements ShareService {

    private final BlogService blogService;
    private final BlogRepository blogRepository;
    private final BlogShareRepository blogShareRepository;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public long share(Long blogId, Long userId) {
        Blog blog = blogService.getVisibleBlog(blogId, userId);
        blogShareRepository.save(BlogShare.builder().blog(blog).userId(userId).build());
        notificationService.notify(blog.getAuthorId(), userId, NotificationType.SHARE, blogId);
        blogRepository.addShareCount(blogId, 1);
        return blogService.getBlog(blogId).getShareCount();
    }
}
