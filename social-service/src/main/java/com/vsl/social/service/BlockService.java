package com.vsl.social.service;

import com.vsl.common.response.PageResponse;
import com.vsl.social.dto.BlockedUserResponse;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Chặn người dùng. Chặn có hiệu lực HAI CHIỀU: hai người không thấy bài viết của nhau, không nhắn tin,
 * không theo dõi, không bình luận / like / chia sẻ bài của nhau. Chỉ người chặn mới mở chặn được.
 * Khi chặn, quan hệ theo dõi giữa hai người (cả hai chiều) bị xóa.
 */
public interface BlockService {

    /** Chặn (idempotent). Người bị chặn phải tồn tại ở identity-service. */
    void block(Long targetId, Long userId);

    /** Mở chặn (idempotent). Quan hệ theo dõi đã xóa lúc chặn KHÔNG được khôi phục. */
    void unblock(Long targetId, Long userId);

    PageResponse<BlockedUserResponse> listBlocked(Long userId, int page, int size);

    /** a chặn b hoặc b chặn a. Một trong hai null -> false (khách không bị chặn). */
    boolean isBlockedBetween(Long a, Long b);

    /** userId đã chặn targetId chưa. */
    boolean hasBlocked(Long userId, Long targetId);

    /** Những người có quan hệ chặn với viewerId (cả hai chiều); khách -> rỗng. */
    Set<Long> hiddenUserIds(Long viewerId);

    /** Danh sách không bao giờ rỗng (thêm -1) để dùng trong "NOT IN :ids" của JPQL. */
    static Collection<Long> forNotIn(Set<Long> ids) {
        return ids.isEmpty() ? List.of(-1L) : ids;
    }
}
