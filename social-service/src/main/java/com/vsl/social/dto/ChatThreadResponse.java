package com.vsl.social.dto;

import java.util.List;

/**
 * Một đoạn lịch sử trò chuyện với partner, sắp cũ -> mới.
 * hasMore = còn tin cũ hơn (gọi lại với before = id tin đầu tiên).
 * blockedByMe = tôi đã chặn partner (FE hiện nút "Bỏ chặn"); blocked = một trong hai người đã chặn người kia.
 */
public record ChatThreadResponse(
        UserBriefResponse partner,
        boolean canMessage,
        List<MessageResponse> messages,
        boolean hasMore,
        boolean blockedByMe,
        boolean blocked
) {
}
