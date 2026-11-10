package com.vsl.identity.service;

import java.util.Collection;

/**
 * Polling Publisher của outbox: gửi các sự kiện PENDING tới service đích, thử lại với backoff lũy thừa.
 */
public interface OutboxRelayService {

    /** Quét và gửi các sự kiện PENDING đã tới hạn (chạy theo lịch). */
    void relay();

    /** Gửi ngay các sự kiện cho trước (bỏ qua hạn chờ; sự kiện không còn PENDING thì bỏ qua). */
    void dispatch(Collection<Long> eventIds);
}
