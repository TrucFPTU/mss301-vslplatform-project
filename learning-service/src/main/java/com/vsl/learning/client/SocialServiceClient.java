package com.vsl.learning.client;

import com.vsl.learning.dto.remote.RemoteBlogStats;
import com.vsl.learning.dto.remote.RemoteResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

/** Gọi social-service (qua Eureka) để lấy số liệu blog cho thống kê tổng hợp. */
@Slf4j
@Component
public class SocialServiceClient {

    private static final ParameterizedTypeReference<RemoteResponse<RemoteBlogStats>> BLOG_STATS_TYPE =
            new ParameterizedTypeReference<>() {};

    private final RestClient restClient;

    public SocialServiceClient(@LoadBalanced RestClient.Builder restClientBuilder,
                               @Value("${social-service.base-url:http://social-service}") String baseUrl,
                               @Value("${social-service.timeout:3s}") Duration timeout) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(timeout)
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(timeout);
        this.restClient = restClientBuilder.baseUrl(baseUrl).requestFactory(requestFactory).build();
    }

    /** Số bài PUBLISHED; null nếu social-service không phản hồi (thống kê không được làm hỏng trang). */
    public Long countPublishedBlogs() {
        try {
            RemoteResponse<RemoteBlogStats> response = restClient.get()
                    .uri("/api/blogs/public-stats")
                    .retrieve()
                    .body(BLOG_STATS_TYPE);
            return response != null && response.data() != null ? response.data().totalPublishedBlogs() : null;
        } catch (Exception e) {
            log.warn("Không lấy được số liệu blog từ social-service: {}", e.getMessage());
            return null;
        }
    }
}
