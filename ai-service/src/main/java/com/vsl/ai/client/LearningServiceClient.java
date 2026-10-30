package com.vsl.ai.client;

import com.vsl.ai.dto.AssistantDtos.LearningContext;
import com.vsl.ai.dto.AssistantDtos.RemoteResponse;
import com.vsl.common.exception.AppException;
import com.vsl.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * Gọi learning-service (qua Eureka) lấy dữ liệu cho trợ lý học tập, CHUYỂN TIẾP JWT của người học
 * → learning chỉ trả tiến độ của chính người đó.
 */
@Slf4j
@Component
public class LearningServiceClient {

    private static final ParameterizedTypeReference<RemoteResponse<LearningContext>> CONTEXT_TYPE =
            new ParameterizedTypeReference<>() {};

    private final RestClient restClient;

    public LearningServiceClient(@LoadBalanced RestClient.Builder restClientBuilder,
                                 @Value("${learning-service.base-url:http://learning-service}") String baseUrl,
                                 @Value("${learning-service.timeout:5s}") Duration timeout) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(timeout)
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(timeout);
        this.restClient = restClientBuilder.baseUrl(baseUrl).requestFactory(requestFactory).build();
    }

    public LearningContext getAssistantContext(String authorization) {
        try {
            RemoteResponse<LearningContext> response = restClient.get()
                    .uri("/api/practice/assistant-context")
                    .header(HttpHeaders.AUTHORIZATION, authorization)
                    .retrieve()
                    .body(CONTEXT_TYPE);
            if (response == null || response.data() == null) {
                throw new AppException(ErrorCode.SERVICE_UNAVAILABLE);
            }
            return response.data();
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Không lấy được dữ liệu học tập từ learning-service: {}", e.getMessage());
            throw new AppException(ErrorCode.SERVICE_UNAVAILABLE);
        }
    }
}
