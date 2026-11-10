package com.vsl.identity.service.saga;

import com.vsl.identity.service.UserDeletionSagaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Lúc khởi động: tạo dòng outbox cho các tài khoản đã xóa TRƯỚC khi có saga, nên deploy là tự dọn,
 * không cần SQL tay. Chạy lại mỗi lần khởi động vô hại (đã có dòng thì bỏ qua).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxBackfillRunner {

    private final UserDeletionSagaService sagaService;

    @EventListener(ApplicationReadyEvent.class)
    public void backfill() {
        try {
            sagaService.backfillDeletedUsers();
        } catch (Exception e) {
            // Không được làm hỏng việc khởi động; lần khởi động sau sẽ thử lại.
            log.error("Bù sự kiện outbox cho tài khoản đã xóa thất bại: {}", e.getMessage(), e);
        }
    }
}
