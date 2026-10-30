package com.vsl.social.dto;

import com.vsl.social.entity.ShareType;

import java.time.Instant;

/** Một bài hiện trên trang cá nhân nhờ được chia sẻ (bởi chính chủ hoặc một người bạn). */
public record SharedBlogResponse(
        Long shareId,
        UserBriefResponse sharedBy,
        ShareType shareType,
        Instant sharedAt,
        BlogResponse blog
) {
}
