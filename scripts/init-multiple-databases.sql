-- Chạy MỘT LẦN khi Postgres khởi tạo volume lần đầu.
-- Mỗi service một database riêng (nguyên tắc database-per-service).
-- Dùng chung 1 Postgres container cho nhẹ (môn học); production nên tách instance.
CREATE DATABASE vsl_identity;
CREATE DATABASE vsl_learning;
CREATE DATABASE vsl_social;
-- ai-service KHONG co DB (stateless).
