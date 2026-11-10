package com.vsl.identity.service.impl;

import com.vsl.identity.entity.OutboxEvent;
import com.vsl.identity.entity.OutboxStatus;
import com.vsl.identity.repository.OutboxEventRepository;
import com.vsl.identity.service.UserDeletionSagaService;
import com.vsl.identity.service.saga.UserDeletionParticipant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OutboxRelayServiceTest {

    @Mock
    private OutboxEventRepository outboxRepository;
    @Mock
    private UserDeletionParticipant social;
    /** Mock trả null cho mọi lời gọi: TransactionTemplate vẫn chạy callback rồi commit(null). */
    @Mock
    private PlatformTransactionManager transactionManager;

    private OutboxRelayServiceImpl relay;

    @BeforeEach
    void setUp() {
        when(social.destination()).thenReturn("social-service");
        relay = new OutboxRelayServiceImpl(outboxRepository, List.of(social), transactionManager);
    }

    private OutboxEvent pending(long id, int attempts, String destination) {
        OutboxEvent e = OutboxEvent.builder()
                .id(id)
                .eventType(UserDeletionSagaService.USER_DELETED)
                .aggregateId(7L)
                .destination(destination)
                .status(OutboxStatus.PENDING)
                .attempts(attempts)
                .nextAttemptAt(LocalDateTime.now().minusSeconds(1))
                .createdAt(LocalDateTime.now())
                .build();
        when(outboxRepository.findById(id)).thenReturn(Optional.of(e));
        return e;
    }

    @Test
    @DisplayName("Gửi thành công -> DONE + processedAt, xóa lỗi cũ")
    void process_Success_MarksDone() {
        OutboxEvent e = pending(1L, 2, "social-service");
        e.setLastError("old");

        relay.process(1L);

        verify(social).cleanup(7L);
        assertThat(e.getStatus()).isEqualTo(OutboxStatus.DONE);
        assertThat(e.getProcessedAt()).isNotNull();
        assertThat(e.getLastError()).isNull();
        verify(outboxRepository).save(e);
    }

    @Test
    @DisplayName("Gửi lỗi -> attempts+1, hẹn lại theo backoff, ghi lỗi")
    void process_Failure_SchedulesRetryWithBackoff() {
        OutboxEvent e = pending(1L, 2, "social-service");
        doThrow(new IllegalStateException("social-service không phản hồi")).when(social).cleanup(7L);

        relay.process(1L);

        assertThat(e.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(e.getAttempts()).isEqualTo(3);
        assertThat(e.getLastError()).contains("IllegalStateException").contains("không phản hồi");
        // Lần lỗi thứ 3 -> 30s * 2^2 = 2 phút.
        assertThat(e.getNextAttemptAt()).isCloseTo(LocalDateTime.now().plusMinutes(2), within(5, java.time.temporal.ChronoUnit.SECONDS));
        verify(outboxRepository).save(e);
    }

    @Test
    @DisplayName("Lỗi lần thứ 12 -> FAILED")
    void process_LastAttempt_MarksFailed() {
        OutboxEvent e = pending(1L, OutboxRelayServiceImpl.MAX_ATTEMPTS - 1, "social-service");
        doThrow(new IllegalStateException("down")).when(social).cleanup(7L);

        relay.process(1L);

        assertThat(e.getStatus()).isEqualTo(OutboxStatus.FAILED);
        assertThat(e.getAttempts()).isEqualTo(OutboxRelayServiceImpl.MAX_ATTEMPTS);
    }

    @Test
    @DisplayName("Không có participant cho destination -> FAILED, không gọi ai")
    void process_UnknownDestination_MarksFailed() {
        OutboxEvent e = pending(1L, 0, "unknown-service");

        relay.process(1L);

        assertThat(e.getStatus()).isEqualTo(OutboxStatus.FAILED);
        assertThat(e.getLastError()).contains("unknown-service");
        verify(social, never()).cleanup(any());
    }

    @Test
    @DisplayName("Sự kiện không còn PENDING (đã DONE) -> bỏ qua")
    void process_NotPending_Skipped() {
        OutboxEvent e = pending(1L, 0, "social-service");
        e.setStatus(OutboxStatus.DONE);

        relay.process(1L);

        verify(social, never()).cleanup(any());
        verify(outboxRepository, never()).save(any());
    }

    @Test
    @DisplayName("relay: lấy các sự kiện tới hạn rồi xử lý từng cái; một cái lỗi không chặn cái sau")
    void relay_ProcessesDueEvents() {
        OutboxEvent a = pending(1L, 0, "social-service");
        OutboxEvent b = pending(2L, 0, "social-service");
        when(outboxRepository.findDue(eq(OutboxStatus.PENDING), any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(List.of(a, b));
        when(outboxRepository.findById(1L)).thenThrow(new IllegalStateException("db"));

        relay.relay();

        assertThat(b.getStatus()).isEqualTo(OutboxStatus.DONE);
        assertThat(a.getStatus()).isEqualTo(OutboxStatus.PENDING);
    }

    @Test
    @DisplayName("Backoff: 30s, 1m, 2m ... tối đa 1 giờ")
    void backoff_GrowsExponentiallyAndCaps() {
        assertThat(OutboxRelayServiceImpl.backoff(1)).isEqualTo(Duration.ofSeconds(30));
        assertThat(OutboxRelayServiceImpl.backoff(2)).isEqualTo(Duration.ofMinutes(1));
        assertThat(OutboxRelayServiceImpl.backoff(4)).isEqualTo(Duration.ofMinutes(4));
        assertThat(OutboxRelayServiceImpl.backoff(8)).isEqualTo(Duration.ofHours(1));
        assertThat(OutboxRelayServiceImpl.backoff(11)).isEqualTo(Duration.ofHours(1));
    }
}
