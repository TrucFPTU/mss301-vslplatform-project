package com.vsl.learning.service;

import com.vsl.learning.client.IdentityServiceClient;
import com.vsl.learning.dto.remote.RemoteStreakReminderRequest;
import com.vsl.learning.repository.AttemptHistoryRepository;
import com.vsl.learning.service.impl.StreakReminderServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StreakReminderServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 7, 10);

    @Mock
    private AttemptHistoryRepository attemptHistoryRepository;
    @Mock
    private IdentityServiceClient identityServiceClient;

    @InjectMocks
    private StreakReminderServiceImpl service;

    @Test
    @DisplayName("Chỉ nhắc người học hôm qua mà CHƯA học hôm nay, kèm streak hiện tại")
    void remindsOnlyUsersWhoHaveNotPracticedToday() {
        // user 1: học 8,9 (chưa học hôm nay 10) -> streak 2, cần nhắc
        // user 2: học 9 và 10 -> đã học hôm nay, KHÔNG nhắc (bản monolith cũ lại nhắc nhầm người này)
        when(attemptHistoryRepository.findUserIdsPracticedBetween(
                eq(TODAY.minusDays(1).atStartOfDay()), eq(TODAY.atStartOfDay()))).thenReturn(List.of(1L, 2L));
        when(attemptHistoryRepository.findUserIdsPracticedBetween(
                eq(TODAY.atStartOfDay()), eq(TODAY.plusDays(1).atStartOfDay()))).thenReturn(List.of(2L));
        when(attemptHistoryRepository.findAttemptTimesByUserId(1L)).thenReturn(List.of(
                TODAY.minusDays(1).atTime(20, 0), TODAY.minusDays(2).atTime(8, 0), TODAY.minusDays(2).atTime(9, 0)));
        when(identityServiceClient.sendStreakReminders(any())).thenReturn(1);

        int sent = service.sendDailyReminders(TODAY);

        ArgumentCaptor<RemoteStreakReminderRequest> captor = ArgumentCaptor.forClass(RemoteStreakReminderRequest.class);
        verify(identityServiceClient).sendStreakReminders(captor.capture());
        assertThat(captor.getValue().date()).isEqualTo(TODAY);
        assertThat(captor.getValue().items()).containsExactly(new RemoteStreakReminderRequest.Item(1L, 2));
        assertThat(sent).isEqualTo(1);
        verify(attemptHistoryRepository, never()).findAttemptTimesByUserId(2L);
    }

    @Test
    @DisplayName("Không có ứng viên -> không gọi identity-service")
    void noCandidates_DoesNotCallIdentity() {
        when(attemptHistoryRepository.findUserIdsPracticedBetween(any(), any())).thenReturn(List.of());

        int sent = service.sendDailyReminders(TODAY);

        assertThat(sent).isZero();
        verifyNoInteractions(identityServiceClient);
    }
}
