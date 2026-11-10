package com.vsl.identity.service.saga;

/**
 * Một service tham gia saga xóa tài khoản (bước "retriable" sau bước pivot ẩn danh hóa ở identity).
 * Mỗi participant = một dòng outbox riêng (cột {@code destination}); thêm service mới chỉ cần thêm
 * một bean hiện thực interface này, không phải sửa relay.
 */
public interface UserDeletionParticipant {

    /** Tên service đích, khớp cột {@code destination} của outbox, vd {@code social-service}. */
    String destination();

    /**
     * Yêu cầu service đích dọn dữ liệu của người dùng đã xóa. Ném exception nếu thất bại (relay sẽ thử lại),
     * nên phía service đích PHẢI idempotent.
     */
    void cleanup(Long userId);
}
