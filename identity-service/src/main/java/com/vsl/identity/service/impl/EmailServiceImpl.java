package com.vsl.identity.service.impl;

import com.vsl.identity.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Gửi email qua Resend API (https://api.resend.com/emails). Bê từ monolith.
 * Thiếu RESEND_API_KEY -> bỏ qua, không làm hỏng luồng đăng ký/đặt lại mật khẩu.
 * (Giữ @Async cho đúng bản gốc; identity-service không bật @EnableAsync nên chạy đồng bộ.)
 */
@Slf4j
@Service
public class EmailServiceImpl implements EmailService {

    @Value("${RESEND_API_KEY:}")
    private String resendApiKey;

    @Value("${RESEND_FROM_EMAIL:SignMentor <onboarding@resend.dev>}")
    private String fromEmail;

    // Nút "Luyện tập ngay" trong email nhắc streak trỏ về FE đang chạy.
    @Value("${APP_FRONTEND_URL:http://localhost:3000}")
    private String frontendUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    @Async
    @Override
    public void sendOtpEmail(String toEmail, String otp) {
        if (resendApiKey == null || resendApiKey.isBlank()) {
            log.warn("Chưa cấu hình RESEND_API_KEY - bỏ qua gửi email OTP cho {}.", toEmail);
            return;
        }

        try {
            String htmlContent = """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 10px;">
                    <h2 style="color: #4f46e5; text-align: center;">SignMentor</h2>
                    <p>Chào bạn,</p>
                    <p>Mã OTP xác thực của bạn là:</p>
                    <div style="background-color: #f3f4f6; padding: 15px; text-align: center; font-size: 24px; font-weight: bold; letter-spacing: 5px; border-radius: 8px; margin: 20px 0;">
                        %s
                    </div>
                    <p style="color: #6b7280; font-size: 14px;">Mã OTP này có hiệu lực trong vòng 5 phút. Vui lòng không chia sẻ mã này cho bất kỳ ai.</p>
                    <p>Nếu bạn không thực hiện yêu cầu này, vui lòng bỏ qua email này.</p>
                    <br/>
                    <p>Trân trọng,<br/>Đội ngũ SignMentor</p>
                </div>
                """.formatted(otp);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(resendApiKey);

            Map<String, Object> body = Map.of(
                    "from", fromEmail,
                    "to", List.of(toEmail),
                    "subject", "Mã Xác Thực OTP - SignMentor",
                    "html", htmlContent
            );

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            restTemplate.postForEntity("https://api.resend.com/emails", entity, String.class);
            log.info("Đã gửi email OTP thành công đến: {} qua Resend API", toEmail);
        } catch (Exception e) {
            log.error("Lỗi khi gửi email OTP đến {} qua Resend API: {}", toEmail, e.getMessage());
        }
    }

    @Async
    @Override
    public void sendStreakReminderEmail(String toEmail, String fullName, int currentStreak) {
        if (resendApiKey == null || resendApiKey.isBlank()) {
            log.warn("Chưa cấu hình RESEND_API_KEY - bỏ qua gửi email nhắc nhở streak cho {}.", toEmail);
            return;
        }
        try {
            String displayName = (fullName != null && !fullName.isBlank()) ? fullName : "bạn";
            String fireEmojis = "🔥".repeat(Math.min(currentStreak, 5));
            String htmlContent = """
                <div style="font-family: 'Segoe UI', Arial, sans-serif; max-width: 600px; margin: 0 auto; background: #0A0E1A; border-radius: 16px; overflow: hidden;">
                  <div style="background: linear-gradient(135deg, #4f46e5 0%%, #7c3aed 100%%); padding: 40px 36px; text-align: center;">
                    <div style="font-size: 56px; margin-bottom: 8px;">%s</div>
                    <h1 style="color: #fff; font-size: 26px; margin: 0; font-weight: 800;">Chuỗi đồng hành SignMentor!</h1>
                    <p style="color: rgba(255,255,255,0.8); margin: 8px 0 0; font-size: 15px;">Đừng để chuỗi học của bạn bị gãy!</p>
                  </div>
                  <div style="padding: 36px; background: #111827;">
                    <p style="color: #e5e7eb; font-size: 16px; line-height: 1.7; margin: 0 0 20px;">Xin chào <strong style="color: #a78bfa;">%s</strong>,</p>
                    <p style="color: #9ca3af; font-size: 15px; line-height: 1.7; margin: 0 0 24px;">
                      Bạn đang có chuỗi học tập liên tiếp <strong style="color: #f59e0b;">%d ngày</strong> rất ấn tượng!
                      Nhưng hiện bạn chưa luyện tập hôm nay và chưa còn nhiều giờ để giữ chuỗi này.
                    </p>
                    <div style="background: linear-gradient(135deg, rgba(79,70,229,0.15) 0%%, rgba(124,58,237,0.15) 100%%); border: 1px solid rgba(79,70,229,0.4); border-radius: 12px; padding: 20px; text-align: center; margin-bottom: 28px;">
                      <div style="font-size: 48px; font-weight: 900; color: #f59e0b;">%d</div>
                      <div style="color: #9ca3af; font-size: 13px; text-transform: uppercase; letter-spacing: 2px;">Ngày liên tiếp</div>
                    </div>
                    <div style="text-align: center; margin-bottom: 28px;">
                      <a href="%s"
                         style="display: inline-block; background: linear-gradient(135deg, #4f46e5 0%%, #7c3aed 100%%); color: #fff; text-decoration: none; font-weight: 700; font-size: 16px; padding: 14px 36px; border-radius: 10px; box-shadow: 0 4px 24px rgba(79,70,229,0.4);">
                        📚 Luyện tập ngay
                      </a>
                    </div>
                    <p style="color: #6b7280; font-size: 13px; text-align: center; line-height: 1.6;">
                      Bạn nhận được email này vì đã bật thông báo nhắc nhở trên SignMentor.<br/>
                      Để tắt thông báo, vào <strong>Trang cá nhân &rarr; Cài đặt thông báo</strong>.
                    </p>
                  </div>
                </div>
                """.formatted(fireEmojis, displayName, currentStreak, currentStreak, frontendUrl);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(resendApiKey);

            Map<String, Object> body = Map.of(
                    "from", fromEmail,
                    "to", List.of(toEmail),
                    "subject", String.format("🔥 Chuỗi %d ngày của bạn sắp bị gãy! - SignMentor", currentStreak),
                    "html", htmlContent
            );

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            restTemplate.postForEntity("https://api.resend.com/emails", entity, String.class);
            log.info("Đã gửi email nhắc nhở streak ({} ngày) cho: {}", currentStreak, toEmail);
        } catch (Exception e) {
            log.error("Lỗi gửi email nhắc nhở streak cho {}: {}", toEmail, e.getMessage());
        }
    }
}
