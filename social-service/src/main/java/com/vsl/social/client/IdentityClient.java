package com.vsl.social.client;

import com.vsl.common.response.PageResponse;
import com.vsl.social.dto.remote.RemoteDtos.RemotePage;
import com.vsl.social.dto.remote.RemoteDtos.RemoteResponse;
import com.vsl.social.dto.remote.RemoteDtos.UserSummary;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Lấy hồ sơ công khai (tên, avatar) từ identity-service. User thuộc identity nên social chỉ lưu userId.
 * identity tạm lỗi -> trả rỗng (tên hiển thị null) chứ không làm hỏng trang.
 */
@Slf4j
@Component
public class IdentityClient {

    private static final int BATCH = 100;
    private static final ParameterizedTypeReference<RemoteResponse<List<UserSummary>>> SUMMARIES =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<RemoteResponse<RemotePage<UserSummary>>> PAGE =
            new ParameterizedTypeReference<>() {};

    private final RestClient restClient;

    public IdentityClient(@LoadBalanced RestClient.Builder builder,
                          @Value("${identity-service.base-url:http://identity-service}") String baseUrl,
                          @Value("${identity-service.timeout:3s}") Duration timeout) {
        HttpClient http = HttpClient.newBuilder().connectTimeout(timeout).version(HttpClient.Version.HTTP_1_1).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(timeout);
        this.restClient = builder.baseUrl(baseUrl).requestFactory(factory).build();
    }

    /** Tra hàng loạt; id không tồn tại không có trong map. */
    public Map<Long, UserSummary> getSummaries(Collection<Long> ids) {
        List<Long> unique = ids.stream().filter(Objects::nonNull).distinct().toList();
        Map<Long, UserSummary> result = new HashMap<>();
        if (unique.isEmpty()) {
            return result;
        }
        try {
            for (int i = 0; i < unique.size(); i += BATCH) {
                List<Long> chunk = unique.subList(i, Math.min(i + BATCH, unique.size()));
                RemoteResponse<List<UserSummary>> response = restClient.get()
                        .uri(u -> u.path("/api/users/summaries").queryParam("ids", chunk.toArray()).build())
                        .retrieve()
                        .body(SUMMARIES);
                if (response != null && response.data() != null) {
                    response.data().forEach(s -> result.put(s.userId(), s));
                }
            }
        } catch (Exception e) {
            log.warn("Không lấy được hồ sơ người dùng từ identity-service: {}", e.getMessage());
        }
        return result;
    }

    public Optional<UserSummary> getSummary(Long id) {
        return Optional.ofNullable(getSummaries(new LinkedHashSet<>(List.of(id))).get(id));
    }

    /**
     * Người dùng có tồn tại không. identity không phản hồi -> coi như có (không chặn thao tác của người dùng
     * chỉ vì service khác tạm lỗi); sai sót hiếm khi đó chỉ là theo dõi/nhắc một id không tồn tại.
     */
    public boolean exists(Long id) {
        try {
            RemoteResponse<List<UserSummary>> response = restClient.get()
                    .uri(u -> u.path("/api/users/summaries").queryParam("ids", id).build())
                    .retrieve()
                    .body(SUMMARIES);
            return response == null || response.data() == null
                    || response.data().stream().anyMatch(s -> id.equals(s.userId()));
        } catch (Exception e) {
            log.warn("Không kiểm tra được user {} ở identity-service: {}", id, e.getMessage());
            return true;
        }
    }

    /** Tìm người dùng theo tên; identity lỗi -> trang rỗng. */
    public PageResponse<UserSummary> search(String keyword, int page, int size) {
        try {
            RemoteResponse<RemotePage<UserSummary>> response = restClient.get()
                    .uri(u -> u.path("/api/users/search")
                            .queryParam("keyword", keyword).queryParam("page", page).queryParam("size", size).build())
                    .retrieve()
                    .body(PAGE);
            if (response != null && response.data() != null) {
                RemotePage<UserSummary> p = response.data();
                return PageResponse.of(p.content(), p.page(), p.size(), p.totalElements());
            }
        } catch (Exception e) {
            log.warn("Không tìm được người dùng ở identity-service: {}", e.getMessage());
        }
        return PageResponse.of(List.of(), page, size, 0);
    }
}
