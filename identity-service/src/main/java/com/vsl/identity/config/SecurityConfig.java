package com.vsl.identity.config;

import com.vsl.common.security.JsonSecurityErrorHandler;
import com.vsl.common.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Bảo mật cho identity-service.
 * Route PUBLIC: đăng ký / đăng nhập / refresh (chưa có token thì mới cần).
 * Route còn lại cần đăng nhập.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JsonSecurityErrorHandler securityErrorHandler;

    /** Dùng để mã hoá mật khẩu khi đăng ký & kiểm tra khi đăng nhập. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/auth/**",
                                "/api/users/public-stats",
                                "/actuator/**",
                                // Swagger / OpenAPI
                                "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**",
                                // Máy phát token dev (chỉ tồn tại khi profile=dev)
                                "/dev/**"
                        ).permitAll()
                        .requestMatchers("/api/users/admin/**", "/api/admin/users/**").hasRole("ADMIN")
                        // API nội bộ giữa các service (token role SERVICE do service tự ký).
                        .requestMatchers("/api/users/internal/**").hasRole("SERVICE")
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
