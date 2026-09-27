package com.vsl.common.devtools;

import com.vsl.common.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * ⚠️ CHỈ DÙNG KHI DEV — chỉ tồn tại khi chạy với profile "dev" (xem DevToolsAutoConfiguration).
 *
 * Sinh nhanh 1 access token hợp lệ để test API có phân quyền, MÀ KHÔNG cần identity-service.
 * Nhờ JWT ký bằng khóa bí mật chung, token này được MỌI service chấp nhận.
 *
 * Dùng: GET /dev/token?userId=1&username=alice&role=ADMIN
 *       → copy "accessToken" dán vào nút Authorize trên Swagger.
 */
@RestController
@RequestMapping("/dev")
@RequiredArgsConstructor
public class DevTokenController {

    private final JwtService jwtService;

    @GetMapping("/token")
    public Map<String, Object> token(
            @RequestParam(defaultValue = "1") Long userId,
            @RequestParam(defaultValue = "devuser") String username,
            @RequestParam(defaultValue = "USER") String role) {

        String accessToken = jwtService.generateAccessToken(userId, username, role);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("userId", userId);
        body.put("username", username);
        body.put("role", role);
        body.put("accessToken", accessToken);
        body.put("usage", "Copy accessToken vào nút Authorize trên Swagger (không kèm chữ 'Bearer').");
        return body;
    }
}
