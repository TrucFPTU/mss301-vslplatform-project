package com.vsl.social.dto;

import com.vsl.social.entity.ShareType;

/** Kết quả chia sẻ: tổng lượt chia sẻ mới + đường dẫn bài để sao chép. */
public record ShareResponse(
        long shareCount,
        String blogUrl,
        ShareType shareType,
        UserBriefResponse recipient
) {
}
