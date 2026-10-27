package com.vsl.learning.client;

import com.vsl.learning.dto.remote.RemoteResponse;
import com.vsl.learning.dto.remote.RemoteUserStats;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

/** Gọi identity-service (qua Eureka): số liệu người dùng. */
@Slf4j
@Component
public class IdentityServiceClient {

    private static final ParameterizedTypeReference<RemoteResponse<RemoteUserStats>> USER_STATS_TYPE =
            new ParameterizedTypeReference<>() {};

    private final RestClient restClient;

    public IdentityServiceClient(@LoadBalanced RestClient.Builder restClientBuilder,
                                 @Value("${identity-service.base-url:http://identity-service}") String baseUrl,
                                 @Value("${identity-service.timeout:3s}") Duration timeout) {
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
}
