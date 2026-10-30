package com.vsl.identity.service;

import com.vsl.identity.dto.StreakReminderRequest;
import com.vsl.identity.dto.StreakReminderResult;

public interface StreakReminderService {

    /**
     * Gửi mail nhắc streak cho các người dùng trong danh sách nếu: đã bật thông báo, tài khoản ACTIVE,
     * streak > 0 và chưa được nhắc trong ngày {@code request.date()}. Đánh dấu đã nhắc sau khi gửi.
     */
    StreakReminderResult sendReminders(StreakReminderRequest request);
}
