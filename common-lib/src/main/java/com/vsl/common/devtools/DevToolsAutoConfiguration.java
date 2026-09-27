package com.vsl.common.devtools;

import com.vsl.common.security.JwtService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

/**
 * Chỉ nạp DevTokenController khi chạy với profile "dev".
 * → Ở môi trường thật (không có profile dev), endpoint /dev/token KHÔNG tồn tại.
 *
 * Bật dev: chạy service với  --spring.profiles.active=dev
 *          hoặc đặt biến môi trường SPRING_PROFILES_ACTIVE=dev
 */
@AutoConfiguration
@Profile("dev")
@ConditionalOnClass(JwtService.class)
public class DevToolsAutoConfiguration {

    @Bean
    public DevTokenController devTokenController(JwtService jwtService) {
        return new DevTokenController(jwtService);
    }
}
