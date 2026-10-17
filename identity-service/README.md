# identity-service (cổng 8081, DB `vsl_identity`)

Định danh & xác thực. Service **duy nhất** phát (ký) JWT.

## Đã có sẵn (hạ tầng)
- Đăng ký Eureka, kéo config, kết nối DB `vsl_identity`
- `SecurityConfig` (route đăng ký/đăng nhập/refresh để public)
- `PasswordEncoder` (BCrypt) đã khai bean
- `JwtService` (ký token) đến từ common-lib — inject là dùng

## Bạn cần code (tạo các package dưới `com.vsl.identity`)
- `entity/`     → `User`, `Role`, `RefreshToken`...
- `repository/` → `UserRepository`...
- `dto/`        → request/response (RegisterRequest, LoginRequest, TokenResponse...)
- `service/`    → nghiệp vụ đăng ký/đăng nhập, gọi `jwtService.generateAccessToken(...)`
- `controller/` → `AuthController` (/api/auth/**), `UserController` (/api/users/**)

## API Quản trị Người dùng (Admin User Management)
Yêu cầu quyền `ADMIN` (qua API Gateway hoặc `X-User-Role: ROLE_ADMIN`).

- `GET /api/users/admin` (hoặc `/api/admin/users`): Danh sách tài khoản người dùng phân trang, tìm kiếm từ khoá (`keyword`), lọc theo vai trò (`role`) và trạng thái (`status`).
- `GET /api/users/admin/statistics`: Thống kê tổng số tài khoản, số tài khoản hoạt động (`ACTIVE`), số tài khoản bị khoá (`BANNED`), số quản trị viên.
- `GET /api/users/admin/{id}`: Xem chi tiết tài khoản người dùng theo ID.
- `POST /api/users/admin`: Admin tạo tài khoản người dùng / quản trị viên mới.
- `PUT /api/users/admin/{id}/status`: Khoá (`BANNED`) hoặc mở khoá (`ACTIVE`) tài khoản người dùng.
- `PUT /api/users/admin/{id}/role`: Phân quyền vai trò (`ROLE_USER` <-> `ROLE_ADMIN`).

