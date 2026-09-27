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

> Gợi ý: tham khảo code monolith cũ (AuthServiceImpl, UserServiceImpl) để bê nghiệp vụ sang.
