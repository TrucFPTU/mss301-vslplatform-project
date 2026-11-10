package com.vsl.identity.entity;

/** Trạng thái một sự kiện trong outbox. */
public enum OutboxStatus {
    /** Chưa gửi được tới service đích (relay sẽ thử lại khi tới hạn). */
    PENDING,
    /** Service đích đã xử lý xong. */
    DONE,
    /** Hết số lần thử (hoặc không có ai xử lý destination này) — cần người xem log. */
    FAILED
}
