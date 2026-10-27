package com.vsl.learning.config;

import com.vsl.common.security.JsonSecurityErrorHandler;
import com.vsl.common.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Bảo mật learning-service.
 * 👉 Người phụ trách tự quyết route nào PUBLIC (vd: xem danh sách từ vựng cho khách).
 * Ví dụ gợi ý đã để sẵn ở permitAll() — sửa theo nhu cầu.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JsonSecurityErrorHandler securityErrorHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/**",
                                "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**",
                                "/dev/**").permitAll()
                        // Xem nội dung học (từ vựng, danh mục) công khai cho khách.
                        // Từ đồng nghĩa gọi Gemini (tốn quota) -> chỉ người đăng nhập, đặt TRƯỚC rule GET công khai.
                        .requestMatchers(HttpMethod.GET, "/api/vocabularies/ai-synonyms").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/vocabularies/**", "/api/categories/**").permitAll()
                        // Số liệu trang chủ + beacon ghi lượt truy cập: khách cũng gọi được.
                        .requestMatchers(HttpMethod.GET, "/api/public/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/visits/track").permitAll()
                        // Quản lý nội dung (tạo/sửa/xoá), API Analytics & Quản trị Thành tựu chỉ ADMIN.
                        .requestMatchers("/api/practice/analytics/**", "/api/practice/admin/**", "/api/achievements/admin/**", "/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/vocabularies/**", "/api/categories/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                // 401 (chưa đăng nhập) / 403 (thiếu quyền) dạng JSON thay vì 403 body rỗng mặc định
                .exceptionHandling(e -> e.authenticationEntryPoint(securityErrorHandler)
                        .accessDeniedHandler(securityErrorHandler))
                .httpBasic(h -> h.disable())
                .formLogin(f -> f.disable())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
