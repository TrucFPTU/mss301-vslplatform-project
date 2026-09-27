# ai-service (cổng 8083, KHÔNG có DB)

Nhận diện ký hiệu — **stateless**: nhận video, trả kết quả chấm điểm. Không lưu gì vào DB.

## Đã có sẵn (hạ tầng)
- Eureka, config, SecurityConfig, thread pool `aiTaskExecutor` (@Async), giới hạn upload 50MB.
- Thư viện ONNX Runtime + JavaCV đã khai trong pom.
- `ai.model.path` trỏ tới `./models/mvitv2_small.onnx` (mount qua volume).

## Bạn cần code (dưới `com.vsl.ai`)
- `service/`    → bê `SignLanguageInferenceService` từ monolith cũ, **bỏ phần ghi DB**
  (AttemptHistory/UserProgress/Achievement) — trả thẳng kết quả về.
- `dto/`        → EvaluationResponse {status, message, confidence, predictedId, rank}
- `controller/` → `AiController` POST `/api/ai/evaluate` nhận `MultipartFile video`, `expectedId`.

## Lưu ý Docker (rủi ro đã biết)
- Base image dùng `eclipse-temurin:21-jre` (glibc), **không dùng alpine**.
- File model đặt trong thư mục `models/` ở gốc repo, mount vào container.
