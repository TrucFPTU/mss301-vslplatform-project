package com.vsl.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * AI SERVICE — nhận diện ký hiệu bằng ONNX + JavaCV.
 *
 * THIẾT KẾ STATELESS (quan trọng):
 *   Service này CHỈ nhận video → trả {predictedId, confidence, status, rank}.
 *   Nó KHÔNG ghi AttemptHistory/UserProgress/Achievement — việc đó do learning-service làm.
 *   Nhờ vậy AI không cần DB, dễ scale, dễ đổi model.
 *
 * TODO (người hiểu AI): bê SignLanguageInferenceService từ monolith cũ sang,
 * bỏ phần persist DB, thêm AiController (/api/ai/evaluate nhận MultipartFile).
 */
@SpringBootApplication
public class AiServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AiServiceApplication.class, args);
    }
}
