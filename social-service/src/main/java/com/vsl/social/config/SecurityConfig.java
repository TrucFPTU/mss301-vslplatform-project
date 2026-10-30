package com.vsl.social.config;

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
 * Bảo mật social-service.
 * 👉 Tự quyết route public (vd: xem danh sách blog cho khách) vs cần đăng nhập (đăng bài, like...).
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
                        // Tuyến quản trị báo cáo (chỉ ADMIN)
                        .requestMatchers("/api/blogs/admin/**", "/api/admin/**").hasRole("ADMIN")
                        // Khách xem được bài viết, bình luận, trả lời, danh sách theo dõi.
                        // (/api/notifications không có trong đây → luôn cần đăng nhập)
                        .requestMatchers(HttpMethod.GET,
                                "/api/blogs", "/api/blogs/**",
                                "/api/comments/**",
                                "/api/follows/**").permitAll()
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
