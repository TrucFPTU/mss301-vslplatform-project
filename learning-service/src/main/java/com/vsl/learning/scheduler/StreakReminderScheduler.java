package com.vsl.learning.scheduler;

import com.vsl.learning.service.StreakReminderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Cron nhắc nhở streak. Mặc định 19:00 giờ VN mỗi ngày (còn 5 tiếng trước khi chuỗi reset lúc 0h).
 * Sửa bản monolith: bản cũ chạy MỖI PHÚT (chế độ demo) và không hề kiểm tra giờ 19h.
 * Đổi lịch qua streak-reminder.cron; tắt hẳn bằng streak-reminder.enabled=false.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StreakReminderScheduler {

    private final StreakReminderService streakReminderService;

    @Value("${streak-reminder.enabled:true}")
    private boolean enabled;

    @Scheduled(cron = "${streak-reminder.cron:0 0 19 * * *}", zone = "${streak-reminder.zone:Asia/Ho_Chi_Minh}")
    public void sendStreakReminders() {
        if (!enabled) {
            return;
        }
        try {
            // Cùng múi giờ với lúc lưu attemptedAt và lúc tính streak ở PracticeService.
            streakReminderService.sendDailyReminders(LocalDate.now());
        } catch (Exception e) {
            log.error("[StreakReminder] Lỗi khi chạy job: {}", e.getMessage(), e);
        }
    }
}
