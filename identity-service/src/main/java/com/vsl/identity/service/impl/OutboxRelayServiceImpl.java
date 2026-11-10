package com.vsl.identity.service.impl;

import com.vsl.identity.entity.OutboxEvent;
import com.vsl.identity.entity.OutboxStatus;
import com.vsl.identity.repository.OutboxEventRepository;
import com.vsl.identity.service.OutboxRelayService;
import com.vsl.identity.service.UserDeletionSagaService;
import com.vsl.identity.service.saga.UserDeletionParticipant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Polling Publisher của outbox. Không giữ transaction trong lúc gọi HTTP: đọc sự kiện, gọi participant,
 * rồi ghi kết quả; mỗi lần đọc/ghi là một transaction ngắn riêng (REQUIRES_NEW).
 * Thử lại với backoff lũy thừa 30s * 2^(lần-1), tối đa 1 giờ; thất bại lần thứ {@value #MAX_ATTEMPTS}
 * thì FAILED (cần người xem log).
 * Chạy 1 instance nên không khóa dòng; scale nhiều instance thì {@code findDue} phải dùng
 * {@code SELECT ... FOR UPDATE SKIP LOCKED}.
 */
@Slf4j
@Service
public class OutboxRelayServiceImpl implements OutboxRelayService {

    static final int MAX_ATTEMPTS = 12;
    static final int BATCH_SIZE = 20;
    static final Duration BASE_BACKOFF = Duration.ofSeconds(30);
    static final Duration MAX_BACKOFF = Duration.ofHours(1);
    private static final int MAX_ERROR_LENGTH = 500;

    private final OutboxEventRepository outboxRepository;
    private final Map<String, UserDeletionParticipant> participants;
    private final TransactionTemplate tx;

    public OutboxRelayServiceImpl(OutboxEventRepository outboxRepository,
                                  List<UserDeletionParticipant> participants,
                                  PlatformTransactionManager transactionManager) {
        this.outboxRepository = outboxRepository;
        this.participants = participants.stream()
                .collect(Collectors.toMap(UserDeletionParticipant::destination, Function.identity()));
        this.tx = new TransactionTemplate(transactionManager);
        this.tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Override
    @Scheduled(fixedDelayString = "${outbox.relay.interval-ms:30000}",
            initialDelayString = "${outbox.relay.initial-delay-ms:20000}")
    public void relay() {
        List<OutboxEvent> due = tx.execute(s ->
                outboxRepository.findDue(OutboxStatus.PENDING, LocalDateTime.now(), PageRequest.of(0, BATCH_SIZE)));
        if (due == null || due.isEmpty()) {
            return;
        }
        dispatch(due.stream().map(OutboxEvent::getId).toList());
    }

    @Override
    public void dispatch(Collection<Long> eventIds) {
        for (Long id : eventIds) {
            try {
                process(id);
            } catch (Exception e) {
                // Lỗi khi đọc/ghi outbox (vd DB chập chờn): bỏ qua, lượt relay sau thử lại.
                log.error("Xử lý sự kiện outbox id={} lỗi: {}", id, e.getMessage(), e);
            }
        }
    }

    void process(Long eventId) {
        OutboxEvent event = tx.execute(s -> outboxRepository.findById(eventId).orElse(null));
        if (event == null || event.getStatus() != OutboxStatus.PENDING) {
            return;
        }
        if (!UserDeletionSagaService.USER_DELETED.equals(event.getEventType())) {
            markFailed(event, "Không hỗ trợ loại sự kiện " + event.getEventType());
            return;
        }
        UserDeletionParticipant participant = participants.get(event.getDestination());
        if (participant == null) {
            markFailed(event, "Không có participant cho destination " + event.getDestination());
            return;
        }
        try {
            participant.cleanup(event.getAggregateId());
        } catch (Exception e) {
            markRetry(event, e);
            return;
        }
        event.setStatus(OutboxStatus.DONE);
        event.setProcessedAt(LocalDateTime.now());
        event.setLastError(null);
        save(event);
        log.info("Outbox id={} ({} userId={} -> {}) DONE sau {} lần lỗi", event.getId(), event.getEventType(),
                event.getAggregateId(), event.getDestination(), event.getAttempts());
    }

    private void markRetry(OutboxEvent event, Exception error) {
        int attempts = event.getAttempts() + 1;
        event.setAttempts(attempts);
        event.setLastError(truncate(error.getClass().getSimpleName() + ": " + error.getMessage()));
        if (attempts >= MAX_ATTEMPTS) {
            event.setStatus(OutboxStatus.FAILED);
            save(event);
            log.error("Outbox id={} ({} userId={} -> {}) FAILED sau {} lần thử: {}", event.getId(),
                    event.getEventType(), event.getAggregateId(), event.getDestination(), attempts, event.getLastError());
            return;
        }
        event.setNextAttemptAt(LocalDateTime.now().plus(backoff(attempts)));
        save(event);
        log.warn("Outbox id={} ({} userId={} -> {}) lỗi lần {}, thử lại lúc {}: {}", event.getId(),
                event.getEventType(), event.getAggregateId(), event.getDestination(), attempts,
                event.getNextAttemptAt(), event.getLastError());
    }

    private void markFailed(OutboxEvent event, String reason) {
        event.setStatus(OutboxStatus.FAILED);
        event.setLastError(truncate(reason));
        save(event);
        log.error("Outbox id={} FAILED: {}", event.getId(), reason);
    }

    /** 30s, 1m, 2m, 4m, ... tối đa 1 giờ. */
    static Duration backoff(int attempts) {
        int shift = Math.min(Math.max(attempts - 1, 0), 20);
        Duration d = BASE_BACKOFF.multipliedBy(1L << shift);
        return d.compareTo(MAX_BACKOFF) > 0 ? MAX_BACKOFF : d;
    }

    private void save(OutboxEvent event) {
        tx.executeWithoutResult(s -> outboxRepository.save(event));
    }

    private static String truncate(String s) {
        if (s == null) {
            return null;
        }
        return s.length() <= MAX_ERROR_LENGTH ? s : s.substring(0, MAX_ERROR_LENGTH);
    }
}
