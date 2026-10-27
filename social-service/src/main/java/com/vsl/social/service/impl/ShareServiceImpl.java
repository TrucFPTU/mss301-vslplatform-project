package com.vsl.social.service.impl;

import com.vsl.common.exception.AppException;
import com.vsl.common.exception.ErrorCode;
import com.vsl.social.dto.ShareRequest;
import com.vsl.social.dto.ShareResponse;
import com.vsl.social.dto.UserBriefResponse;
import com.vsl.social.entity.Blog;
import com.vsl.social.entity.BlogShare;
import com.vsl.social.entity.BlogStatus;
import com.vsl.social.entity.NotificationType;
import com.vsl.social.entity.ShareType;
import com.vsl.social.repository.BlogRepository;
import com.vsl.social.repository.BlogShareRepository;
import com.vsl.social.repository.UserFollowRepository;
import com.vsl.social.service.BlogService;
import com.vsl.social.service.NotificationService;
import com.vsl.social.service.ShareService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * Bê từ monolith (shareBlog). Sửa so với bản cũ:
 * - chỉ chia sẻ được bài đang công khai (cũ cho share cả nháp / bài đã gỡ);
 * - blogUrl trỏ đúng bài (cũ trả cứng trang chủ).
 */
@Service
public class ShareServiceImpl implements ShareService {

    private final BlogService blogService;
    private final BlogRepository blogRepository;
    private final BlogShareRepository blogShareRepository;
    private final UserFollowRepository followRepository;
    private final NotificationService notificationService;
    private final SocialViews views;
    private final String frontendUrl;

    public ShareServiceImpl(BlogService blogService, BlogRepository blogRepository,
                            BlogShareRepository blogShareRepository, UserFollowRepository followRepository,
                            NotificationService notificationService, SocialViews views,
                            @Value("${app.frontend-url:https://www.sighmentor.click}") String frontendUrl) {
        this.blogService = blogService;
        this.blogRepository = blogRepository;
        this.blogShareRepository = blogShareRepository;
        this.followRepository = followRepository;
        this.notificationService = notificationService;
        this.views = views;
        this.frontendUrl = frontendUrl.replaceAll("/+$", "");
    }

    @Override
    @Transactional
    public ShareResponse share(Long blogId, Long userId, ShareRequest request) {
        Blog blog = blogService.getVisibleBlog(blogId, userId);
        if (blog.getStatus() != BlogStatus.PUBLISHED) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Chỉ chia sẻ được bài viết đang công khai");
        }
        ShareType type = request == null || request.shareType() == null ? ShareType.COPY_URL : request.shareType();
        Long recipientId = type == ShareType.PROFILE && request != null ? request.recipientUserId() : null;

        if (recipientId != null) {
            if (Objects.equals(recipientId, userId)) {
                throw new AppException(ErrorCode.CANNOT_SHARE_TO_SELF);
            }
            boolean friends = followRepository.existsByFollowerIdAndFolloweeId(userId, recipientId)
                    && followRepository.existsByFollowerIdAndFolloweeId(recipientId, userId);
            if (!friends) {
                throw new AppException(ErrorCode.NOT_FRIEND);
            }
        }

        blogShareRepository.save(BlogShare.builder()
                .blog(blog)
                .userId(userId)
                .shareType(type)
                .recipientId(recipientId)
                .build());
        notificationService.notify(blog.getAuthorId(), userId, NotificationType.SHARE, blogId);
        if (recipientId != null && !Objects.equals(recipientId, blog.getAuthorId())) {
            notificationService.notify(recipientId, userId, NotificationType.SHARE, blogId);
        }
        blogRepository.addShareCount(blogId, 1);

        UserBriefResponse recipient = recipientId == null ? null : views.user(recipientId);
        return new ShareResponse(blogService.getBlog(blogId).getShareCount(),
                frontendUrl + "/blogs/" + blogId, type, recipient);
    }
}
