package com.vsl.ai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Thread pool riêng cho suy luận AI, để tác vụ nặng (giải mã video + ONNX)
 * KHÔNG chiếm thread phục vụ HTTP của Tomcat.
 * Dùng qua @Async("aiTaskExecutor") trên phương thức inference.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "aiTaskExecutor")
    public Executor aiTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("ai-infer-");
        executor.initialize();
        return executor;
    }
}
