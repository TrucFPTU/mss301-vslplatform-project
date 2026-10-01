package com.vsl.learning.client;

import com.vsl.common.exception.AppException;
import com.vsl.common.exception.ErrorCode;
import com.vsl.learning.dto.ai.AiServiceResponse;
import com.vsl.learning.dto.ai.EvaluationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Collection;

/**
 * Client gọi ai-service để chấm điểm video luyện tập: POST http://ai-service/api/ai/evaluate (multipart).
 * ai-service stateless -> learning-service tự lưu lịch sử sau khi nhận kết quả.
 */
@Slf4j
@Component
public class AiServiceClient {

    private static final String EVALUATE_PATH = "/api/ai/evaluate";
    private static final ParameterizedTypeReference<AiServiceResponse<EvaluationResponse>> EVALUATION_TYPE =
            new ParameterizedTypeReference<>() {};

    private final RestClient restClient;

    public AiServiceClient(@LoadBalanced RestClient.Builder restClientBuilder,
                           @Value("${ai-service.base-url:http://ai-service}") String baseUrl,
                           @Value("${ai-service.connect-timeout:5s}") Duration connectTimeout,
                           @Value("${ai-service.read-timeout:120s}") Duration readTimeout) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(connectTimeout)
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(readTimeout);

        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                // ai-service tự verify JWT -> chuyển tiếp token của request hiện tại.
                .requestInterceptor((request, body, execution) -> {
                    String authorization = currentAuthorizationHeader();
                    if (authorization != null) {
                        request.getHeaders().set(HttpHeaders.AUTHORIZATION, authorization);
                    }
                    return execution.execute(request, body);
                })
                .build();
    }

    /**
     * @param video      video luyện tập của người dùng
     * @param expectedId class AI tương ứng với từ cần luyện
     * @param startFrac  vị trí bắt đầu động tác (0..1)
     * @param endFrac    vị trí kết thúc động tác (0..1)
     * @param validIds   các class hợp lệ (từ có video mẫu) để ai-service chỉ chấm trong phạm vi này
     */
    public EvaluationResponse evaluate(MultipartFile video, int expectedId, float startFrac, float endFrac,
                                       Collection<Integer> validIds) {
        MultipartBodyBuilder body = new MultipartBodyBuilder();
        body.part("video", video.getResource())
                .contentType(MediaType.parseMediaType(video.getContentType()));
        body.part("expectedId", expectedId);
        body.part("startFrac", startFrac);
        body.part("endFrac", endFrac);
        validIds.forEach(id -> body.part("validIds", id));

        AiServiceResponse<EvaluationResponse> response;
        try {
            response = restClient.post()
                    .uri(EVALUATE_PATH)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body.build())
                    .retrieve()
                    .body(EVALUATION_TYPE);
        } catch (RestClientResponseException e) {
            throw translateErrorResponse(e);
        } catch (RestClientException | IllegalStateException e) {
            // Không kết nối được / timeout / Eureka không có instance ai-service nào.
            log.error("ai-service không phản hồi: {}", e.getMessage());
            throw new AppException(ErrorCode.AI_MODEL_NOT_LOADED,
                    "Dịch vụ chấm điểm AI hiện không khả dụng, vui lòng thử lại sau");
        }

        if (response == null || response.getData() == null || response.getData().getStatus() == null) {
            throw new AppException(ErrorCode.AI_INFERENCE_ERROR, "ai-service trả về kết quả không hợp lệ");
        }
        return response.getData();
    }

    /** Giữ nguyên lỗi nghiệp vụ của ai-service (vd video rỗng/sai định dạng) để client hiểu nguyên nhân. */
    private AppException translateErrorResponse(RestClientResponseException e) {
        String message = null;
        try {
            AiServiceResponse<?> error = e.getResponseBodyAs(AiServiceResponse.class);
            message = error != null ? error.getMessage() : null;
        } catch (Exception ignored) {
            // body không phải JSON chuẩn -> dùng message mặc định
        }
        log.warn("ai-service trả lỗi {}: {}", e.getStatusCode(), message);

        int status = e.getStatusCode().value();
        if (status == 400) {
            return new AppException(ErrorCode.VALIDATION_ERROR, message != null ? message : "Video không hợp lệ");
        }
        if (status == 503) {
            return new AppException(ErrorCode.AI_MODEL_NOT_LOADED, message != null ? message : "Model AI chưa sẵn sàng");
        }
        return new AppException(ErrorCode.AI_INFERENCE_ERROR,
                message != null ? message : "Lỗi khi chấm điểm video");
    }

    private static String currentAuthorizationHeader() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
        }
        return null;
    }
}
