package com.vsl.learning.config;

import com.vsl.common.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

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
                        .requestMatchers(HttpMethod.GET, "/api/vocabularies/**", "/api/categories/**").permitAll()
                        // Quản lý nội dung (tạo/sửa/xoá) chỉ ADMIN.
                        .requestMatchers("/api/vocabularies/**", "/api/categories/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .httpBasic(h -> h.disable())
                .formLogin(f -> f.disable())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
