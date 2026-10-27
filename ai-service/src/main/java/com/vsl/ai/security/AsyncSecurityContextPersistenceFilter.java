package com.vsl.ai.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Lưu SecurityContext (do {@code JwtAuthenticationFilter} của common-lib nạp vào holder)
 * sang {@code SecurityContextRepository} để nó SỐNG SÓT qua ASYNC dispatch.
 *
 * <p>VÌ SAO CẦN: AiController trả {@code CompletableFuture} → Spring MVC làm 2 lần dispatch.
 * JwtAuthenticationFilter là OncePerRequestFilter nên KHÔNG chạy lại ở lần dispatch thứ 2
 * (ghi response), và nó cũng không tự lưu context vào repository. Nếu không lưu, ở lần
 * dispatch async thứ 2 {@code SecurityContextHolderFilter} khôi phục được context rỗng →
 * {@code AuthorizationFilter} trả 401/403 dù token hoàn toàn hợp lệ.
 *
 * <p>Filter này chạy NGAY SAU JwtAuthenticationFilter trên lần dispatch đầu: nếu đã có
 * authentication thì lưu vào {@link SecurityContextRepository} (RequestAttribute...).
 * Lần dispatch async thứ 2, SecurityContextHolderFilter đọc lại từ đó → context còn nguyên.
 */
@RequiredArgsConstructor
public class AsyncSecurityContextPersistenceFilter extends OncePerRequestFilter {

    private final SecurityContextRepository securityContextRepository;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        SecurityContext context = SecurityContextHolder.getContext();
        if (context.getAuthentication() != null
                && context.getAuthentication().isAuthenticated()
                && !securityContextRepository.containsContext(request)) {
            securityContextRepository.saveContext(context, request, response);
        }
        filterChain.doFilter(request, response);
    }
}
