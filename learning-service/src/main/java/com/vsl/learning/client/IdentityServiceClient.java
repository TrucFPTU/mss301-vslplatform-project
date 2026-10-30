package com.vsl.learning.client;

import com.vsl.common.security.JwtService;
import com.vsl.learning.dto.remote.RemoteResponse;
import com.vsl.learning.dto.remote.RemoteStreakReminderRequest;
import com.vsl.learning.dto.remote.RemoteStreakReminderResult;
import com.vsl.learning.dto.remote.RemoteUserStats;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

/** Gọi identity-service (qua Eureka): số liệu người dùng + gửi nhắc streak. */
@Slf4j
@Component
public class IdentityServiceClient {

    private static final ParameterizedTypeReference<RemoteResponse<RemoteUserStats>> USER_STATS_TYPE =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<RemoteResponse<RemoteStreakReminderResult>> STREAK_RESULT_TYPE =
            new ParameterizedTypeReference<>() {};

    /** Danh tính dùng khi learning-service tự gọi (không có người dùng đứng sau, vd cron job). */
    private static final long SERVICE_PRINCIPAL_ID = 0L;
    private static final String SERVICE_NAME = "learning-service";
    private static final String SERVICE_ROLE = "SERVICE";

    private final RestClient restClient;
    private final JwtService jwtService;

    public IdentityServiceClient(@LoadBalanced RestClient.Builder restClientBuilder,
                                 JwtService jwtService,
                                 @Value("${identity-service.base-url:http://identity-service}") String baseUrl,
                                 @Value("${identity-service.timeout:3s}") Duration timeout) {
        this.jwtService = jwtService;
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(timeout)
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(timeout);
        this.restClient = restClientBuilder.baseUrl(baseUrl).requestFactory(requestFactory).build();
    }

    /** Tổng người dùng + học viên; null nếu identity-service không phản hồi. */
    public RemoteUserStats getUserStats() {
        try {
            RemoteResponse<RemoteUserStats> response = restClient.get()
                    .uri("/api/users/public-stats")
                    .retrieve()
                    .body(USER_STATS_TYPE);
            return response != null ? response.data() : null;
        } catch (Exception e) {
            log.warn("Không lấy được số liệu người dùng từ identity-service: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Gửi danh sách ứng viên nhắc streak; identity lọc theo cài đặt thông báo rồi gửi mail.
     * Gọi bằng token role SERVICE do learning-service tự ký (JWT_SECRET dùng chung).
     *
     * @return số mail đã gửi; -1 nếu identity-service không phản hồi
     */
    public int sendStreakReminders(RemoteStreakReminderRequest request) {
        String serviceToken = jwtService.generateAccessToken(SERVICE_PRINCIPAL_ID, SERVICE_NAME, SERVICE_ROLE);
        try {
            RemoteResponse<RemoteStreakReminderResult> response = restClient.post()
                    .uri("/api/users/internal/streak-reminders")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + serviceToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(STREAK_RESULT_TYPE);
            return response != null && response.data() != null ? response.data().sent() : 0;
        } catch (Exception e) {
            log.error("Gửi nhắc streak sang identity-service thất bại: {}", e.getMessage());
            return -1;
        }
    }
}
