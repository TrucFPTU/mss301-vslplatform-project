package com.vsl.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vsl.common.exception.AppException;
import com.vsl.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Gọi Gemini generateContent ở chế độ JSON, dùng chung cho kiểm duyệt và trợ lý học tập.
 * Model chính quá tải / treo -> thử lần lượt các model dự phòng.
 */
@Slf4j
@Component
public class GeminiClient {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    private final String apiKey;
    private final String model;
    /** Model dự phòng (phân tách dấu phẩy) - thử lần lượt khi model chính quá tải (429/503) hoặc không phản hồi. */
    private final List<String> fallbackModels;
    private final boolean enabled;

    public GeminiClient(@Value("${gemini.api-key:}") String apiKey,
                        @Value("${gemini.model:gemini-3.5-flash-lite}") String model,
                        @Value("${gemini.fallback-models:gemini-3.1-flash-lite,gemini-2.5-flash-lite}") List<String> fallbackModels,
                        @Value("${gemini.enabled:true}") boolean enabled) {
        this.apiKey = apiKey;
        this.model = model;
        this.fallbackModels = fallbackModels == null ? List.of() : fallbackModels;
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled && apiKey != null && !apiKey.isBlank();
    }

    /** Gọi Gemini (JSON mode), trả raw body. Lỗi mạng / HTTP != 200 ở mọi model -> AppException(errorCode). */
    public String generateJson(String prompt, double temperature, ErrorCode errorCode) {
        String requestBody;
        try {
            requestBody = objectMapper.writeValueAsString(Map.of(
                    "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
                    "generationConfig", Map.of("temperature", temperature, "responseMimeType", "application/json")));
        } catch (Exception e) {
            throw new AppException(errorCode);
        }

        // Google thỉnh thoảng báo một model "quá tải" (503) / giới hạn tốc độ (429) / treo: đó là lỗi tạm thời của
        // RIÊNG model đó, nên thử model dự phòng trước khi kết luận Gemini không khả dụng.
        List<String> models = new ArrayList<>();
        models.add(model);
        fallbackModels.stream().map(String::trim).filter(m -> !m.isEmpty() && !models.contains(m)).forEach(models::add);
        for (String m : models) {
            HttpResponse<String> response;
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://generativelanguage.googleapis.com/v1beta/models/" + m + ":generateContent"))
                        .timeout(Duration.ofSeconds(25))
                        .header("Content-Type", "application/json")
                        .header("x-goog-api-key", apiKey)
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .build();
                response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AppException(errorCode);
            } catch (Exception e) {
                log.warn("Gọi Gemini ({}) thất bại: {} - thử model kế tiếp", m, e.getMessage());
                continue;
            }
            int code = response.statusCode();
            if (code == 200) {
                return response.body();
            }
            if (code == 429 || code == 503 || code == 500 || code == 404) {
                log.warn("Gemini ({}) trả HTTP {} - thử model kế tiếp: {}", m, code, truncate(response.body()));
                continue;
            }
            // 400/401/403: lỗi cấu hình (khóa sai, request sai) - đổi model cũng không khỏi.
            log.error("Gemini ({}) trả HTTP {}: {}", m, code, truncate(response.body()));
            throw new AppException(errorCode);
        }
        log.error("Mọi model Gemini đều không khả dụng: {}", models);
        throw new AppException(errorCode);
    }

    /** Đọc JSON trong candidates[0].content.parts[0].text (bỏ ```json fence nếu có). */
    public JsonNode parseCandidateJson(JsonNode candidates, ErrorCode errorCode) {
        if (!candidates.isArray() || candidates.isEmpty()) {
            throw new AppException(errorCode);
        }
        String text = candidates.get(0).path("content").path("parts").path(0).path("text").asText("");
        if (text.isBlank()) {
            throw new AppException(errorCode);
        }
        try {
            return objectMapper.readTree(stripCodeFence(text));
        } catch (Exception e) {
            log.error("Không đọc được JSON trong phản hồi Gemini: {}", truncate(text));
            throw new AppException(errorCode);
        }
    }

    public JsonNode readTree(String body, ErrorCode errorCode) {
        try {
            return objectMapper.readTree(body);
        } catch (Exception e) {
            log.error("Phản hồi Gemini không phải JSON: {}", truncate(body));
            throw new AppException(errorCode);
        }
    }

    private static String stripCodeFence(String text) {
        String t = text.trim();
        if (t.startsWith("```")) {
            int firstNewline = t.indexOf('\n');
            if (firstNewline >= 0) t = t.substring(firstNewline + 1);
            if (t.endsWith("```")) t = t.substring(0, t.length() - 3);
        }
        return t.trim();
    }

    static String truncate(String s) {
        if (s == null) return "";
        return s.length() <= 500 ? s : s.substring(0, 500) + "...";
    }
}
