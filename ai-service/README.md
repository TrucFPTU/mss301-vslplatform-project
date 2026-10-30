# ai-service — Chấm điểm ký hiệu (STATELESS)

Nhận video ký hiệu → model ONNX (MViTv2-Small) chấm **CORRECT / ALMOST_CORRECT / INCORRECT**.
**KHÔNG có DB**: service này chỉ suy luận, không ghi lịch sử — learning-service tự lưu sau khi nhận kết quả.

## API

```
POST /api/ai/evaluate            (multipart/form-data, cần JWT hợp lệ)
  video      : file video (bắt buộc)
  expectedId : int    — class AI của từ cần luyện (bắt buộc)
  startFrac  : float  — vị trí bắt đầu động tác 0..1 (mặc định 0.0)
  endFrac    : float  — vị trí kết thúc động tác 0..1 (mặc định 1.0)
  validIds   : int[]  — các class CÓ VIDEO mẫu, để softmax MASKED (tùy chọn)

→ ApiResponse<{ status, message, confidence, predictedId, rank }>
```

learning-service gọi endpoint này qua `AiServiceClient` (Eureka + load-balanced), truyền sẵn `validIds`
(các `expectedId` của từ có video) nên ai-service không cần truy vấn DB.

## Những điểm NHẠY CẢM — đừng sửa nếu không hiểu rõ (bê nguyên từ monolith)

- **3 cấu hình ONNX bắt buộc** (`initModel`): `setMemoryPatternOptimization(false)` (model có Loop node
  shape động — bật sẽ sai từ run thứ 2), `setIntraOpNumThreads(1)` (bit-exact, hết rung confidence),
  `ALL_OPT`. Model gốc phải chạy qua `scripts/fix_onnx_model.py` của monolith.
- **Decode TUẦN TỰ** toàn bộ frame rồi lấy mẫu đều 16 frame trong `[startFrac,endFrac]` — KHÔNG seek
  keyframe (`setVideoFrameNumber`) vì bất định & lệch pipeline train.
- **Chuẩn hóa Kinetics** `(px/255 - 0.45)/0.225`, tensor **CTHW** `[1,3,16,224,224]`, resize cạnh ngắn
  224 + center-crop.
- **Softmax MASKED**: chỉ tính trên `validIds` (các từ có video), bỏ qua các lớp nhiễu → rank/confidence
  phản ánh đúng phạm vi app. `validIds` rỗng → fallback softmax 1000 lớp.
- **Xếp loại**: CORRECT (rank==1 & prob>50%), ALMOST_CORRECT (rank 2..5), INCORRECT (rank>5).
- **Async + bảo mật**: controller trả `CompletableFuture` chạy trên `aiTaskExecutor` (core=4/max=8/queue=50;
  queue đầy → 503 `AI_SERVICE_BUSY`). Vì async 2 lần dispatch, `SecurityConfig` dùng
  `RequestAttributeSecurityContextRepository` + `AsyncSecurityContextPersistenceFilter` để context sống sót
  qua dispatch thứ 2 (nếu không sẽ 401/403 giả dù token hợp lệ).

## Model

File `models/mvitv2_small.onnx` (~135MB) **KHÔNG commit**; đặt vào `models/` ở gốc repo, mount vào container
qua `AI_MODEL_PATH` (docker-compose đã cấu hình `./models:/app/models:ro`).
Chưa có model → `/api/ai/evaluate` trả 503 `AI_MODEL_NOT_LOADED` (app vẫn khởi động bình thường).

## Chạy dev

```bash
docker compose up -d eureka-server config-server
mvn -pl ai-service spring-boot:run -Dspring-boot.run.profiles=dev
# Swagger: http://localhost:8083/swagger-ui.html
```
