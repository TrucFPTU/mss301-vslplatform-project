package com.vsl.learning.service.impl;

import com.vsl.learning.client.IdentityServiceClient;
import com.vsl.learning.dto.remote.RemoteStreakReminderRequest;
import com.vsl.learning.repository.AttemptHistoryRepository;
import com.vsl.learning.service.PracticeStreakCalculator;
import com.vsl.learning.service.StreakReminderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Phần "chọn ai cần nhắc" của nhắc nhở streak (learning có dữ liệu luyện tập).
 * Sửa lỗi bản monolith: query cũ lọc nhầm người ĐÃ học hôm nay (EXISTS thay vì NOT EXISTS).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StreakReminderServiceImpl implements StreakReminderService {

    private final AttemptHistoryRepository attemptHistoryRepository;
    private final IdentityServiceClient identityServiceClient;

    @Override
    @Transactional(readOnly = true)
    public int sendDailyReminders(LocalDate today) {
        LocalDateTime startOfToday = today.atStartOfDay();
        // Đã học hôm qua -> streak còn sống; chưa học hôm nay -> sắp gãy nếu không học trước 0h.
        Set<Long> practicedYesterday = new HashSet<>(attemptHistoryRepository.findUserIdsPracticedBetween(
                today.minusDays(1).atStartOfDay(), startOfToday));
        practicedYesterday.removeAll(attemptHistoryRepository.findUserIdsPracticedBetween(
                startOfToday, today.plusDays(1).atStartOfDay()));

        if (practicedYesterday.isEmpty()) {
            log.info("[StreakReminder] Không có ai cần nhắc ngày {}.", today);
            return 0;
        }

        List<RemoteStreakReminderRequest.Item> items = new ArrayList<>();
        for (Long userId : practicedYesterday) {
            List<LocalDate> datesDesc = attemptHistoryRepository.findAttemptTimesByUserId(userId).stream()
                    .map(LocalDateTime::toLocalDate)
                    .distinct()
                    .sorted(Comparator.reverseOrder())
                    .toList();
            int current = PracticeStreakCalculator.computeStreak(datesDesc, today).current();
            if (current > 0) {
                items.add(new RemoteStreakReminderRequest.Item(userId, current));
            }
        }

        int sent = identityServiceClient.sendStreakReminders(new RemoteStreakReminderRequest(today, items));
        log.info("[StreakReminder] Ngày {}: {} ứng viên, identity báo đã gửi {}.", today, items.size(), sent);
        return sent;
    }
}
