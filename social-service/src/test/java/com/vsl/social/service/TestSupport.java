package com.vsl.social.service;

import com.vsl.common.exception.AppException;
import com.vsl.common.exception.ErrorCode;
import com.vsl.social.entity.Blog;
import com.vsl.social.entity.BlogComment;
import com.vsl.social.entity.BlogStatus;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Dữ liệu mẫu + đăng nhập giả (giống JwtAuthenticationFilter: principal = userId, authority = ROLE_<role>). */
final class TestSupport {

    static final Long AUTHOR = 1L;
    static final Long OTHER = 2L;
    static final Long ADMIN = 99L;

    private TestSupport() {
    }

    static void loginAs(Long userId, String role) {
        var auth = new UsernamePasswordAuthenticationToken(userId, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role)));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    static void logout() {
        SecurityContextHolder.clearContext();
    }

    static Blog blog(Long id, Long authorId, BlogStatus status) {
        Blog b = Blog.builder()
                .authorId(authorId)
                .title("Title " + id)
                .content("Content " + id)
                .status(status)
                .build();
        b.setId(id);
        return b;
    }

    static BlogComment comment(Long id, Blog blog, Long userId) {
        BlogComment c = BlogComment.builder().blog(blog).userId(userId).content("comment " + id).build();
        c.setId(id);
        return c;
    }

    static void assertAppError(ThrowingCallable call, ErrorCode expected) {
        assertThatThrownBy(call)
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(expected);
    }
}
