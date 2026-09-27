package com.vsl.identity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * IDENTITY SERVICE — định danh & xác thực.
 * Là service DUY NHẤT phát (ký) JWT khi đăng nhập.
 *
 * TODO cho người phụ trách: tạo entity User/Role, repository, service,
 * và controller (/api/auth/register, /login, /refresh, /me; /api/users/**).
 * JwtService (ký token) đã có sẵn trong common-lib — chỉ việc inject và dùng.
 */
@SpringBootApplication
public class IdentityServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(IdentityServiceApplication.class, args);
    }
}
