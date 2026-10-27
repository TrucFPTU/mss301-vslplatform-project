package com.vsl.ai.controller;

import com.vsl.ai.dto.EvaluationResponse;
import com.vsl.ai.service.SignLanguageInferenceService;
import com.vsl.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Chấm điểm ký hiệu (STATELESS). learning-service gọi sang để lấy kết quả rồi tự lưu lịch sử.
 *
 * Yêu cầu JWT hợp lệ (SecurityConfig: anyRequest authenticated). ai-service KHÔNG cần userId
 * vì không ghi DB — chỉ xác thực để tránh bị gọi ẩn danh.
 *
 * Trả {@code CompletableFuture} → Spring MVC xử lý bất đồng bộ trên aiTaskExecutor,
 * không chiếm thread Tomcat trong lúc inference (2-5s).
 */
@Slf4j
@Tag(name = "AI", description = "Chấm điểm video ký hiệu bằng model ONNX")
@RestController
@RequiredArgsConstructor
public class AiController {

    private final SignLanguageInferenceService inferenceService;

    @Operation(summary = "Chấm điểm video luyện tập",
            description = "Nhận video (multipart) + expectedId + cửa sổ động tác [startFrac,endFrac] "
                    + "+ validIds (các class có video mẫu để softmax MASKED). Trả status/confidence/predictedId/rank.")
    @PostMapping(value = "/api/ai/evaluate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CompletableFuture<ApiResponse<EvaluationResponse>> evaluate(
            @RequestPart("video") MultipartFile video,
            @RequestParam("expectedId") int expectedId,
            @RequestParam(value = "startFrac", required = false, defaultValue = "0.0") float startFrac,
            @RequestParam(value = "endFrac", required = false, defaultValue = "1.0") float endFrac,
            @RequestParam(value = "validIds", required = false) List<Integer> validIds) {

        log.info("Evaluate request queued: expectedId={}, fileSize={}KB, window=[{},{}], validIds={}",
                expectedId, video.getSize() / 1024, startFrac, endFrac,
                validIds == null ? 0 : validIds.size());

        return inferenceService.evaluate(video, expectedId, startFrac, endFrac, validIds)
                .thenApply(result -> ApiResponse.ok(result, "Đánh giá hoàn tất"));
    }
}
