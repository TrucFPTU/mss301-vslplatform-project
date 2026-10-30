package com.vsl.identity.service;

/** Gửi email qua Resend API (HTTP). Không cấu hình RESEND_API_KEY thì tự bỏ qua (no-op). */
public interface EmailService {

    void sendOtpEmail(String toEmail, String otp);

    void sendStreakReminderEmail(String toEmail, String fullName, int currentStreak);
}
