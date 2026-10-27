package com.vsl.social.client;

import com.vsl.common.exception.AppException;
import com.vsl.common.exception.ErrorCode;
import com.vsl.social.dto.remote.RemoteDtos.DuplicateCheckRequest;
import com.vsl.social.dto.remote.RemoteDtos.DuplicateCheckResult;
import com.vsl.social.dto.remote.RemoteDtos.ExistingBlog;
import com.vsl.social.dto.remote.RemoteDtos.ModerationRequest;
import com.vsl.social.dto.remote.RemoteDtos.ModerationResult;
import com.vsl.social.dto.remote.RemoteDtos.RemoteResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;

/**
 * Gọi ai-service (Gemini) để kiểm duyệt nội dung và phát hiện bài đăng lại.
 * Mặc định FAIL-CLOSED như monolith: ai-service lỗi/không phản hồi -> chặn với MODERATION_ERROR (503),
 * tránh lọt nội dung chưa kiểm. moderation.fail-open=true để cho đăng khi chạy dev không có ai-service.
 */
@Slf4j
@Component
public class AiModerationClient {

    private static final ParameterizedTypeReference<RemoteResponse<ModerationResult>> MODERATION =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<RemoteResponse<DuplicateCheckResult>> DUPLICATE =
            new ParameterizedTypeReference<>() {};

    private final RestClient restClient;
    private final boolean failOpen;

    public AiModerationClient(@LoadBalanced RestClient.Builder builder,
                              @Value("${ai-service.base-url:http://ai-service}") String baseUrl,
                              @Value("${ai-service.timeout:40s}") Duration timeout,
                              @Value("${moderation.fail-open:false}") boolean failOpen) {
        HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5))
                .version(HttpClient.Version.HTTP_1_1).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(timeout);
        this.failOpen = failOpen;
        this.restClient = builder.baseUrl(baseUrl).requestFactory(factory)
                // ai-service tự verify JWT -> chuyển tiếp token của người đang đăng bài.
                .requestInterceptor((request, body, execution) -> {
                    String auth = currentAuthorization();
                    if (auth != null) {
                        request.getHeaders().set(HttpHeaders.AUTHORIZATION, auth);
                    }
                    return execution.execute(request, body);
                })
                .build();
    }

    public ModerationResult moderate(String title, String content) {
        try {
            RemoteResponse<ModerationResult> response = restClient.post().uri("/api/ai/moderation")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new ModerationRequest(title, content))
                    .retrieve().body(MODERATION);
            if (response == null || response.data() == null) {
                return unavailable("phản hồi rỗng");
            }
            return response.data();
        } catch (RestClientResponseException e) {
            return unavailable("HTTP " + e.getStatusCode().value());
        } catch (Exception e) {
            return unavailable(e.getMessage());
        }
    }

    public DuplicateCheckResult checkDuplicate(String title, String content, List<ExistingBlog> existing) {
        if (existing.isEmpty()) {
            return new DuplicateCheckResult(false, null, "", false);
        }
        try {
            RemoteResponse<DuplicateCheckResult> response = restClient.post().uri("/api/ai/moderation/duplicate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new DuplicateCheckRequest(title, content, existing))
                    .retrieve().body(DUPLICATE);
            if (response != null && response.data() != null) {
                return response.data();
            }
            failOrSkip("phản hồi rỗng");
        } catch (RestClientResponseException e) {
            failOrSkip("HTTP " + e.getStatusCode().value());
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            failOrSkip(e.getMessage());
        }
        return new DuplicateCheckResult(false, null, "", false);
    }

    private ModerationResult unavailable(String detail) {
        failOrSkip(detail);
        return new ModerationResult(true, "", false);
    }

    private void failOrSkip(String detail) {
        if (!failOpen) {
            log.error("Kiểm duyệt nội dung không khả dụng ({}) - chặn đăng (fail-closed).", detail);
            throw new AppException(ErrorCode.MODERATION_ERROR);
        }
        log.warn("Kiểm duyệt nội dung không khả dụng ({}) - cho đăng vì moderation.fail-open=true.", detail);
    }

    private static String currentAuthorization() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            return attrs.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
        }
        return null;
    }
}
