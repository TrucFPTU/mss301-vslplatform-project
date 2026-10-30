package com.vsl.learning.dto.remote;

import java.time.LocalDate;
import java.util.List;

/** Body gửi identity-service POST /api/users/internal/streak-reminders. */
public record RemoteStreakReminderRequest(LocalDate date, List<Item> items) {

    public record Item(Long userId, int currentStreak) {
    }
}
