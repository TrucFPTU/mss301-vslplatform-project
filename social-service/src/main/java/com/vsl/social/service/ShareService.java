package com.vsl.social.service;

public interface ShareService {

    /** Ghi nhận một lượt chia sẻ, trả về tổng số lượt chia sẻ mới. */
    long share(Long blogId, Long userId);
}
