package com.vsl.identity.client;

import com.vsl.common.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;

/**
 * Gọi social-service (qua Eureka) bằng token role SERVICE do identity tự ký (JWT_SECRET dùng chung).
 * KHÔNG nuốt lỗi: HTTP khác 2xx / lỗi mạng ném exception để relay của outbox ghi lỗi và thử lại.
 */
@Component
public class SocialServiceClient {

    private static final ParameterizedTypeReference<Map<String, Object>> JSON = new ParameterizedTypeReference<>() {};

    /** Danh tính khi identity tự gọi (không có người dùng đứng sau). */
    private static final long SERVICE_PRINCIPAL_ID = 0L;
    private static final String SERVICE_NAME = "identity-service";
    private static final String SERVICE_ROLE = "SERVICE";

    private final RestClient restClient;
    private final JwtService jwtService;

    public SocialServiceClient(@LoadBalanced RestClient.Builder restClientBuilder,
                               JwtService jwtService,
                               @Value("${social-service.base-url:http://social-service}") String baseUrl,
                               @Value("${social-service.timeout:3s}") Duration timeout) {
        this.jwtService = jwtService;
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(timeout)
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(timeout);
        this.restClient = restClientBuilder.baseUrl(baseUrl).requestFactory(requestFactory).build();
    }

    /**
     * Báo social dọn dữ liệu của người dùng đã xóa (idempotent ở phía social).
     *
     * @return phần {@code data} của phản hồi (số follows / chặn / thông báo đã xóa, số bài đã ẩn) — để ghi log
     */
    public Object notifyUserDeleted(Long userId) {
        String token = jwtService.generateAccessToken(SERVICE_PRINCIPAL_ID, SERVICE_NAME, SERVICE_ROLE);
        Map<String, Object> body = restClient.post()
                .uri("/api/internal/users/{userId}/deleted", userId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .body(JSON);
        return body != null ? body.get("data") : null;
    }
}
