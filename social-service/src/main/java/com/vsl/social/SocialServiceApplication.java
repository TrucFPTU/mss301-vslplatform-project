package com.vsl.social;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * SOCIAL SERVICE — blog & tương tác xã hội.
 * Sở hữu: Blog, BlogComment, BlogLike, BlogShare, BlogReport,
 *         CommentReply, UserFollow, BlogNotification...
 *
 * TODO: tạo entity/repository/service/controller. Nhớ tham chiếu người dùng
 * bằng Long userId (không @ManyToOne tới User của identity-service).
 */
@SpringBootApplication
public class SocialServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(SocialServiceApplication.class, args);
    }
}
