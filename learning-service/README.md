# learning-service (cổng 8082, DB `vsl_learning`)

Nội dung học & tiến trình. Điều phối luồng luyện tập (gọi ai-service chấm điểm, rồi tự ghi lịch sử).

## Cấu trúc (`com.vsl.learning`)
- `entity/`     → Vocabulary, Category, UserProgress, AttemptHistory, Achievement, UserAchievement, LearningStatus
  (KHÔNG `@ManyToOne User` — chỉ lưu `Long userId`)
- `repository/` → Spring Data JPA cho từng entity
- `dto/`        → `request/`, `response/`, `ai/` (hợp đồng với ai-service)
- `service/`    → Category, Vocabulary, Attempt, Practice, Achievement (+ PracticeStreakCalculator)
- `client/AiServiceClient` → gọi `http://ai-service/api/ai/evaluate` qua RestClient `@LoadBalanced`
- `config/`     → SecurityConfig, RestClientConfig

## API
| Method | Path | Quyền |
|---|---|---|
| GET | `/api/categories`, `/api/categories/{id}` | Công khai |
| POST/PUT/DELETE | `/api/categories[/{id}]` | ADMIN |
| GET | `/api/vocabularies`, `/{id}`, `/search?keyword=`, `/category/{categoryId}` | Công khai |
| POST/PUT/DELETE | `/api/vocabularies[/{id}]` | ADMIN |
| POST | `/api/practice/evaluate` (multipart: `video`, `expectedId`, `startFrac`, `endFrac`) | Đăng nhập |
| GET | `/api/practice/progress`, `/api/practice/stats`, `/api/practice/achievements` | Đăng nhập |
| GET | `/api/attempts`, `/api/attempts/recent?limit=` | Đăng nhập |

## Luồng luyện tập
nhận video → kiểm tra video + tìm từ vựng theo `expectedId` → gọi ai-service
(gửi kèm `validIds` = các expectedId có video mẫu, và chuyển tiếp header `Authorization`)
→ nhận `{status, confidence, predictedId, rank, message}`
→ ghi AttemptHistory + cập nhật UserProgress (LEARNING → LEARNED, không hạ cấp) + mở khoá Achievement.

ai-service không phản hồi / không có instance → 503 `AI_MODEL_NOT_LOADED`.

## Hợp đồng chờ ai-service
`POST /api/ai/evaluate` (multipart) nhận: `video`, `expectedId`, `startFrac`, `endFrac`, `validIds` (lặp lại nhiều lần),
trả `ApiResponse<EvaluationResponse>` với `data = {status: CORRECT|ALMOST_CORRECT|INCORRECT, message, confidence, predictedId, rank}`.
`validIds` thay cho việc ai-service đọc bảng vocabularies (ai-service không có DB).

## Chạy dev
```bash
mvn -DskipTests -pl common-lib -am install     # lần đầu: cài common-lib vào ~/.m2
docker compose up -d postgres eureka-server config-server
mvn -pl learning-service spring-boot:run -Dspring-boot.run.profiles=dev
```
