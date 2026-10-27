package com.vsl.learning.service;

import java.time.LocalDate;

public interface StreakReminderService {

    /**
     * Chọn người đã luyện tập hôm qua nhưng CHƯA luyện tập hôm nay (streak sắp gãy), tính streak hiện tại
     * rồi chuyển sang identity-service gửi mail.
     *
     * @return số mail identity báo đã gửi; 0 nếu không có ứng viên; -1 nếu identity không phản hồi
     */
    int sendDailyReminders(LocalDate today);
}
