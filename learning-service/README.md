# learning-service (cổng 8082, DB `vsl_learning`)

Nội dung học & tiến trình. Điều phối luồng luyện tập (gọi ai-service chấm điểm, rồi tự ghi lịch sử).

## Đã có sẵn (hạ tầng)
- Eureka, config, DB `vsl_learning`, SecurityConfig, load-balancer để gọi ai-service.

## Bạn cần code (dưới `com.vsl.learning`)
- `entity/`     → Vocabulary, Category, UserProgress, AttemptHistory, Achievement, UserAchievement
  (nhớ: KHÔNG `@ManyToOne User` — chỉ lưu `Long userId`)
- `repository/`, `dto/`, `service/`, `controller/`
- Client gọi ai-service: dùng `RestClient`/`@LoadBalanced` tới `http://ai-service/api/ai/evaluate`

> Luồng luyện tập: nhận video → gọi ai-service → nhận {predictedId, confidence, status}
> → learning-service ghi AttemptHistory + cập nhật UserProgress + mở khoá Achievement.
