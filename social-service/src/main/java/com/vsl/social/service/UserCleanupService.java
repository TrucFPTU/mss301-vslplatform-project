package com.vsl.social.service;

import com.vsl.social.dto.UserCleanupResponse;

/**
 * Bước "retriable" của saga xóa tài khoản (identity điều phối, gọi qua API nội bộ).
 * Dọn dữ liệu xã hội của người đã xóa: gỡ theo dõi và chặn (cả hai chiều), xóa thông báo gửi tới họ,
 * ẩn bài viết của họ. GIỮ bình luận / trả lời / like / chia sẻ / tố cáo / tin nhắn (đã hiển thị
 * "Người dùng đã xóa"; giữ mạch hội thoại và số đếm).
 */
public interface UserCleanupService {

    /** Lý do ghi vào bài bị ẩn vì tác giả đã xóa tài khoản. */
    String AUTHOR_DELETED_REASON = "Tác giả đã xóa tài khoản";

    /**
     * Idempotent: gọi lại trả toàn số 0, không lỗi. Không gọi ngược identity (identity là nguồn sự thật);
     * userId social không biết cũng trả 0.
     */
    UserCleanupResponse cleanupDeletedUser(Long userId);
}
