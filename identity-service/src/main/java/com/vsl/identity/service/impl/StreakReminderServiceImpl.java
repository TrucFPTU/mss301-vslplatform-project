package com.vsl.identity.service.impl;

import com.vsl.identity.dto.StreakReminderRequest;
import com.vsl.identity.dto.StreakReminderResult;
import com.vsl.identity.entity.User;
import com.vsl.identity.repository.UserRepository;
import com.vsl.identity.service.EmailService;
import com.vsl.identity.service.StreakReminderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Phần "gửi mail" của nhắc nhở streak (bản monolith gộp trong StreakReminderScheduler). */
@Slf4j
@Service
@RequiredArgsConstructor
public class StreakReminderServiceImpl implements StreakReminderService {

    private final UserRepository userRepository;
    private final EmailService emailService;

    @Override
    @Transactional
    public StreakReminderResult sendReminders(StreakReminderRequest request) {
        LocalDate date = request.date();
        Map<Long, Integer> streakByUser = request.items().stream()
                .filter(item -> item.currentStreak() > 0)
                .collect(Collectors.toMap(StreakReminderRequest.Item::userId,
                        StreakReminderRequest.Item::currentStreak, Math::max));
        if (streakByUser.isEmpty()) {
            return new StreakReminderResult(0);
        }

        Map<Long, User> users = userRepository.findAllById(streakByUser.keySet()).stream()
                .collect(Collectors.toMap(User::getUserId, Function.identity()));

        int sent = 0;
        for (Map.Entry<Long, Integer> entry : streakByUser.entrySet()) {
            User user = users.get(entry.getKey());
            if (user == null || !user.isEmailNotificationsEnabled() || !user.isActive()
                    || date.equals(user.getStreakReminderSentDate())) {
                continue;
            }
            try {
                emailService.sendStreakReminderEmail(user.getEmail(), user.getFullName(), entry.getValue());
                // Đánh dấu đã nhắc trong ngày để lần quét sau không gửi trùng
                user.setStreakReminderSentDate(date);
                userRepository.save(user);
                sent++;
            } catch (Exception e) {
                // Lỗi của 1 người không được làm hỏng cả lượt gửi
                log.error("[StreakReminder] Lỗi khi gửi cho userId={}: {}", user.getUserId(), e.getMessage());
            }
        }
        log.info("[StreakReminder] Đã gửi {} mail nhắc streak (ngày {}, {} ứng viên).", sent, date, streakByUser.size());
        return new StreakReminderResult(sent);
    }
}
