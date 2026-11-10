package com.vsl.identity.service.impl;

import com.vsl.identity.entity.OutboxEvent;
import com.vsl.identity.entity.OutboxStatus;
import com.vsl.identity.entity.UserStatus;
import com.vsl.identity.repository.OutboxEventRepository;
import com.vsl.identity.repository.UserRepository;
import com.vsl.identity.service.OutboxRelayService;
import com.vsl.identity.service.UserDeletionSagaService;
import com.vsl.identity.service.saga.UserDeletionParticipant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Điều phối saga xóa tài khoản. {@link #start} nhập chung transaction ẩn danh hóa (propagation REQUIRED) nên
 * dòng outbox và việc đổi trạng thái user cùng commit hoặc cùng rollback.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserDeletionSagaServiceImpl implements UserDeletionSagaService {

    /**
     * Hạn relay của sự kiện mới: lùi một chút vì afterCommit đã gửi ngay một lần, tránh relay gửi trùng
     * cùng lúc. Gửi ngay thất bại thì sự kiện được hẹn lại theo backoff như mọi lần thử khác.
     */
    static final Duration IMMEDIATE_GRACE = Duration.ofSeconds(30);

    private final OutboxEventRepository outboxRepository;
    private final UserRepository userRepository;
    private final List<UserDeletionParticipant> participants;
    private final OutboxRelayService relayService;

    @Override
    @Transactional
    public void start(Long userId) {
        List<Long> created = createEvents(userId, LocalDateTime.now().plus(IMMEDIATE_GRACE));
        if (created.isEmpty()) {
            return;
        }
        log.info("Saga xóa tài khoản userId={}: ghi {} sự kiện outbox", userId, created.size());
        dispatchAfterCommit(created);
    }

    @Override
    @Transactional
    public int backfillDeletedUsers() {
        LocalDateTime now = LocalDateTime.now();
        int count = 0;
        for (Long userId : userRepository.findIdsByStatus(UserStatus.DELETED)) {
            count += createEvents(userId, now).size();
        }
        if (count > 0) {
            log.info("Saga xóa tài khoản: bù {} sự kiện outbox cho các tài khoản đã xóa trước đó", count);
        }
        return count;
    }

    /** Một dòng PENDING cho mỗi participant chưa có dòng (idempotent; unique constraint chặn thêm lần nữa). */
    private List<Long> createEvents(Long userId, LocalDateTime nextAttemptAt) {
        List<Long> ids = new ArrayList<>();
        for (UserDeletionParticipant participant : participants) {
            String destination = participant.destination();
            if (outboxRepository.existsByEventTypeAndAggregateIdAndDestination(USER_DELETED, userId, destination)) {
                continue;
            }
            OutboxEvent event = outboxRepository.save(OutboxEvent.builder()
                    .eventType(USER_DELETED)
                    .aggregateId(userId)
                    .destination(destination)
                    .status(OutboxStatus.PENDING)
                    .attempts(0)
                    .nextAttemptAt(nextAttemptAt)
                    .createdAt(LocalDateTime.now())
                    .build());
            ids.add(event.getId());
        }
        return ids;
    }

    /**
     * Sau commit thì gửi ngay một lần (thường xong trong chưa tới 1 giây). Lỗi chỉ ghi log: dòng outbox vẫn
     * PENDING, relay sẽ thử lại. Relay tự mở transaction mới (REQUIRES_NEW) cho mỗi lần đọc/ghi, vì trong
     * afterCommit tài nguyên của transaction cũ vẫn còn gắn với thread.
     */
    private void dispatchAfterCommit(List<Long> eventIds) {
        Runnable dispatch = () -> {
            try {
                relayService.dispatch(eventIds);
            } catch (Exception e) {
                log.warn("Gửi ngay sự kiện outbox {} thất bại, relay sẽ thử lại: {}", eventIds, e.getMessage());
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    dispatch.run();
                }
            });
        } else {
            dispatch.run();
        }
    }
}
