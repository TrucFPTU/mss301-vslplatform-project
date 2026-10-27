package com.vsl.ai.config;

import com.vsl.ai.security.AsyncSecurityContextPersistenceFilter;
import com.vsl.common.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

/**
 * Bảo mật ai-service: chấm điểm cần đăng nhập (JWT hợp lệ).
 *
 * Endpoint /api/ai/evaluate trả CompletableFuture (async) → PHẢI dùng
 * {@link RequestAttributeSecurityContextRepository} để SecurityContext sống sót
 * qua ASYNC dispatch, nếu không sẽ bị 401/403 giả ở lần dispatch thứ 2 dù token hợp lệ.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    /** Lưu context vào request attribute (vẫn stateless - không dùng session). */
    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new RequestAttributeSecurityContextRepository();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        SecurityContextRepository contextRepository = securityContextRepository();
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Context sống sót qua ASYNC dispatch (AiController trả CompletableFuture).
                .securityContext(sc -> sc.securityContextRepository(contextRepository))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/**",
                                "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**",
                                "/dev/**").permitAll()
                        .anyRequest().authenticated()
                )
                .httpBasic(h -> h.disable())
                .formLogin(f -> f.disable())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                // Ngay sau khi JWT nạp context: lưu vào repository để async dispatch khôi phục được.
                .addFilterAfter(new AsyncSecurityContextPersistenceFilter(contextRepository),
                        JwtAuthenticationFilter.class);
        return http.build();
    }
}
