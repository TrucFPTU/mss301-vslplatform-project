package com.vsl.identity.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

/**
 * learning-service gửi sang: những người đã học hôm qua nhưng CHƯA học hôm nay, kèm streak hiện tại.
 * identity-service lọc tiếp theo cài đặt thông báo / trạng thái / đã nhắc hôm nay chưa rồi gửi mail.
 *
 * @param date ngày nhắc (do learning tính) - dùng để chống gửi trùng trong cùng ngày
 */
public record StreakReminderRequest(
        @NotNull LocalDate date,
        @NotNull @Valid List<Item> items
) {
    public record Item(@NotNull Long userId, int currentStreak) {
    }
}
