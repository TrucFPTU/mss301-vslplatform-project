package com.vsl.social.dto;

import com.vsl.social.entity.ShareType;

/**
 * shareType: COPY_URL (mặc định) hoặc PROFILE.
 * recipientUserId: chỉ dùng với PROFILE - đăng lên trang của một người bạn; bỏ trống = trang của chính mình.
 */
public record ShareRequest(ShareType shareType, Long recipientUserId) {
}
