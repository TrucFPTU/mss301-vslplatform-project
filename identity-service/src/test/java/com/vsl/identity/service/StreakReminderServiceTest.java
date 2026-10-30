package com.vsl.identity.service;

import com.vsl.identity.dto.StreakReminderRequest;
import com.vsl.identity.dto.StreakReminderResult;
import com.vsl.identity.entity.Role;
import com.vsl.identity.entity.User;
import com.vsl.identity.entity.UserStatus;
import com.vsl.identity.repository.UserRepository;
import com.vsl.identity.service.impl.StreakReminderServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StreakReminderServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 7, 10);

    @Mock
    private UserRepository userRepository;
    @Mock
    private EmailService emailService;

    @InjectMocks
    private StreakReminderServiceImpl service;

    private User user(long id, boolean notifications, UserStatus status, LocalDate sentDate) {
        return User.builder()
                .userId(id).username("u" + id).email("u" + id + "@vsl.com").fullName("User " + id)
                .passwordHash("x").role(Role.USER).status(status)
                .emailNotificationsEnabled(notifications).streakReminderSentDate(sentDate)
                .build();
    }

    @Test
    @DisplayName("Chỉ gửi cho người bật thông báo, ACTIVE, chưa nhắc hôm nay; đánh dấu đã nhắc")
    void sendsOnlyToEligibleUsers() {
        User eligible = user(1, true, UserStatus.ACTIVE, TODAY.minusDays(3));
        User notificationsOff = user(2, false, UserStatus.ACTIVE, null);
        User banned = user(3, true, UserStatus.BANNED, null);
        User alreadyReminded = user(4, true, UserStatus.ACTIVE, TODAY);
        when(userRepository.findAllById(any())).thenReturn(List.of(eligible, notificationsOff, banned, alreadyReminded));

        StreakReminderResult result = service.sendReminders(new StreakReminderRequest(TODAY, List.of(
                new StreakReminderRequest.Item(1L, 5),
                new StreakReminderRequest.Item(2L, 3),
                new StreakReminderRequest.Item(3L, 3),
                new StreakReminderRequest.Item(4L, 3))));

        assertThat(result.sent()).isEqualTo(1);
        verify(emailService).sendStreakReminderEmail("u1@vsl.com", "User 1", 5);
        verify(emailService, times(1)).sendStreakReminderEmail(anyString(), anyString(), anyInt());
        assertThat(eligible.getStreakReminderSentDate()).isEqualTo(TODAY);
        verify(userRepository).save(eligible);
    }

    @Test
    @DisplayName("Streak <= 0 bị bỏ qua, không truy vấn DB")
    void zeroStreak_Skipped() {
        StreakReminderResult result = service.sendReminders(new StreakReminderRequest(TODAY,
                List.of(new StreakReminderRequest.Item(1L, 0))));

        assertThat(result.sent()).isZero();
        verifyNoInteractions(userRepository, emailService);
    }
}
