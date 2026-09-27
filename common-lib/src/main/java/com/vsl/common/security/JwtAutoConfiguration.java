package com.vsl.common.security;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Auto-configuration: chỉ cần service NÀO thêm common-lib (và có Spring Security trên classpath),
 * các bean JWT sẽ TỰ ĐỘNG có sẵn — không phải khai báo lại ở từng service.
 *
 * Đây là "phép màu" giúp đồng đội chỉ việc include common-lib là dùng được JWT ngay.
 * Việc còn lại của mỗi service: trong SecurityConfig của mình, gắn JwtAuthenticationFilter
 * vào chuỗi lọc và khai báo route nào public / route nào cần đăng nhập.
 */
@AutoConfiguration
@ConditionalOnClass(SecurityFilterChain.class)
@EnableConfigurationProperties(JwtProperties.class)
public class JwtAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public JwtService jwtService(JwtProperties properties) {
        return new JwtService(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtService jwtService) {
        return new JwtAuthenticationFilter(jwtService);
    }
}
