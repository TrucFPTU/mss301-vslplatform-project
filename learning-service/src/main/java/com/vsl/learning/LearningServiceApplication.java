package com.vsl.learning;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * LEARNING SERVICE — nội dung học & tiến trình.
 * Sở hữu: Vocabulary, Category, UserProgress, AttemptHistory, Achievement.
 * Là service ĐIỀU PHỐI luồng luyện tập: gọi ai-service để chấm điểm,
 * rồi TỰ ghi AttemptHistory / UserProgress / Achievement.
 *
 * TODO: tạo entity/repository/service/controller cho các nghiệp vụ trên.
 */
@SpringBootApplication
public class LearningServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(LearningServiceApplication.class, args);
    }
}
