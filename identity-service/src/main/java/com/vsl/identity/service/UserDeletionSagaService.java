package com.vsl.identity.service;

/**
 * Saga xóa tài khoản (orchestration, identity điều phối).
 * T1 (pivot, identity): ẩn danh hóa user + ghi sự kiện {@code USER_DELETED} vào outbox trong CÙNG một transaction.
 * T2 (retriable, mỗi participant): dọn dữ liệu ở service khác; relay thử lại tới khi thành công.
 * Không có bước bù trừ: xóa tài khoản là việc người dùng muốn chắc chắn xảy ra -> phục hồi tiến.
 */
public interface UserDeletionSagaService {

    String USER_DELETED = "USER_DELETED";

    /**
     * Gọi TRONG transaction ẩn danh hóa: ghi một dòng outbox PENDING cho mỗi participant (đã có thì bỏ qua);
     * sau khi commit thì thử gửi ngay một lần (lỗi thì để relay thử lại).
     */
    void start(Long userId);

    /**
     * Tạo dòng outbox cho các tài khoản đã xóa TRƯỚC khi có saga (chạy lúc khởi động, idempotent).
     *
     * @return số dòng outbox mới tạo
     */
    int backfillDeletedUsers();
}
