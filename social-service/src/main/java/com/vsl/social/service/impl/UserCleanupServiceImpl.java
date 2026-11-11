package com.vsl.social.service.impl;

import com.vsl.social.dto.UserCleanupResponse;
import com.vsl.social.entity.BlogStatus;
import com.vsl.social.repository.BlogNotificationRepository;
import com.vsl.social.repository.BlogRepository;
import com.vsl.social.repository.UserBlockRepository;
import com.vsl.social.repository.UserFollowRepository;
import com.vsl.social.service.UserCleanupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bài đã bị admin gỡ (HIDDEN) từ trước giữ nguyên lý do gỡ của admin. Không xóa ảnh bìa (bài chỉ bị ẩn).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserCleanupServiceImpl implements UserCleanupService {

    private final UserFollowRepository followRepository;
    private final UserBlockRepository blockRepository;
    private final BlogNotificationRepository notificationRepository;
    private final BlogRepository blogRepository;

    @Override
    @Transactional
    public UserCleanupResponse cleanupDeletedUser(Long userId) {
        int follows = followRepository.deleteAllInvolving(userId);
        int blocks = blockRepository.deleteAllInvolving(userId);
        int notifications = notificationRepository.deleteByRecipient(userId);
        int blogs = blogRepository.hideAllByAuthor(userId, BlogStatus.HIDDEN, AUTHOR_DELETED_REASON);
        UserCleanupResponse result = new UserCleanupResponse(follows, blocks, notifications, blogs);
        log.info("Dọn dữ liệu người dùng đã xóa userId={}: {}", userId, result);
        return result;
    }
}
