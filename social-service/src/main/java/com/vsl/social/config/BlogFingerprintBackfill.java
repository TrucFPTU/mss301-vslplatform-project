package com.vsl.social.config;

import com.vsl.social.entity.Blog;
import com.vsl.social.repository.BlogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Bài tạo trước khi có cột normalized_title / content_fingerprint thì 2 cột này đang null -> chặn đăng trùng
 * sẽ bỏ sót các bài đó. Khởi động: tính bổ sung một lần (bài mới đã được Blog tự tính khi lưu).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BlogFingerprintBackfill implements ApplicationRunner {

    private final BlogRepository blogRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<Blog> missing = blogRepository.findByContentFingerprintIsNull();
        if (missing.isEmpty()) {
            return;
        }
        missing.forEach(Blog::refreshFingerprints);
        blogRepository.saveAll(missing);
        log.info("Đã bổ sung dạng chuẩn hóa cho {} bài viết cũ.", missing.size());
    }
}
