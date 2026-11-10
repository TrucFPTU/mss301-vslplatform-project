package com.vsl.identity.service;

import com.vsl.identity.entity.OutboxEvent;
import com.vsl.identity.entity.OutboxStatus;
import com.vsl.identity.entity.UserStatus;
import com.vsl.identity.repository.OutboxEventRepository;
import com.vsl.identity.repository.UserRepository;
import com.vsl.identity.service.impl.UserDeletionSagaServiceImpl;
import com.vsl.identity.service.saga.UserDeletionParticipant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static com.vsl.identity.service.UserDeletionSagaService.USER_DELETED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDeletionSagaServiceTest {

    @Mock
    private OutboxEventRepository outboxRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private OutboxRelayService relayService;

    private UserDeletionSagaServiceImpl saga;
    private final AtomicLong ids = new AtomicLong(100);

    @BeforeEach
    void setUp() {
        saga = new UserDeletionSagaServiceImpl(outboxRepository, userRepository,
                List.of(participant("social-service"), participant("learning-service")), relayService);
    }

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private static UserDeletionParticipant participant(String destination) {
        UserDeletionParticipant p = mock(UserDeletionParticipant.class);
        when(p.destination()).thenReturn(destination);
        return p;
    }

    private void stubSaveAssignsId() {
        when(outboxRepository.save(any(OutboxEvent.class))).thenAnswer(inv -> {
            OutboxEvent e = inv.getArgument(0);
            e.setId(ids.incrementAndGet());
            return e;
        });
    }

    @Test
    @DisplayName("start: mỗi participant một dòng PENDING, gửi ngay SAU commit")
    void start_CreatesOnePendingRowPerParticipant_DispatchesAfterCommit() {
        stubSaveAssignsId();
        TransactionSynchronizationManager.initSynchronization();

        saga.start(7L);

        ArgumentCaptor<OutboxEvent> saved = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxRepository, times(2)).save(saved.capture());
        assertThat(saved.getAllValues()).extracting(OutboxEvent::getDestination)
                .containsExactly("social-service", "learning-service");
        assertThat(saved.getAllValues()).allSatisfy(e -> {
            assertThat(e.getEventType()).isEqualTo(USER_DELETED);
            assertThat(e.getAggregateId()).isEqualTo(7L);
            assertThat(e.getStatus()).isEqualTo(OutboxStatus.PENDING);
            assertThat(e.getAttempts()).isZero();
            assertThat(e.getNextAttemptAt()).isAfter(LocalDateTime.now());
        });

        // Chưa commit -> chưa gửi.
        verify(relayService, never()).dispatch(anyCollection());
        List<TransactionSynchronization> syncs = TransactionSynchronizationManager.getSynchronizations();
        assertThat(syncs).hasSize(1);
        syncs.getFirst().afterCommit();
        verify(relayService).dispatch(List.of(101L, 102L));
    }

    @Test
    @DisplayName("start gọi lại cho cùng user -> không tạo trùng, không gửi")
    void start_Idempotent() {
        when(outboxRepository.existsByEventTypeAndAggregateIdAndDestination(eq(USER_DELETED), eq(7L), any()))
                .thenReturn(true);

        saga.start(7L);

        verify(outboxRepository, never()).save(any());
        verify(relayService, never()).dispatch(anyCollection());
    }

    @Test
    @DisplayName("Gửi ngay lỗi -> chỉ ghi log (dòng outbox vẫn PENDING cho relay)")
    void start_ImmediateDispatchFailureIsSwallowed() {
        stubSaveAssignsId();
        doThrow(new IllegalStateException("social down")).when(relayService).dispatch(anyCollection());

        // Không có transaction đang chạy -> gửi luôn; lỗi không được lan ra người gọi.
        saga.start(7L);

        verify(relayService).dispatch(anyCollection());
    }

    @Test
    @DisplayName("Backfill: user DELETED chưa có dòng outbox -> tạo; đã có -> bỏ qua")
    void backfill_CreatesMissingRowsOnly() {
        stubSaveAssignsId();
        when(userRepository.findIdsByStatus(UserStatus.DELETED)).thenReturn(List.of(5L, 6L));
        when(outboxRepository.existsByEventTypeAndAggregateIdAndDestination(eq(USER_DELETED), eq(5L), any()))
                .thenReturn(true);

        int created = saga.backfillDeletedUsers();

        assertThat(created).isEqualTo(2);
        ArgumentCaptor<OutboxEvent> saved = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxRepository, times(2)).save(saved.capture());
        assertThat(saved.getAllValues()).extracting(OutboxEvent::getAggregateId).containsOnly(6L);
        assertThat(saved.getAllValues()).allSatisfy(e ->
                assertThat(e.getNextAttemptAt()).isBeforeOrEqualTo(LocalDateTime.now()));
        // Backfill không gửi ngay; relay sẽ gửi ở lượt đầu tiên.
        verify(relayService, never()).dispatch(anyCollection());
    }
}
