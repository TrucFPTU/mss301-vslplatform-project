package com.vsl.ai.config;

import com.vsl.common.exception.AppException;
import com.vsl.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Thread pool riêng cho suy luận AI, để tác vụ nặng (giải mã video + ONNX)
 * KHÔNG chiếm thread phục vụ HTTP của Tomcat.
 * Dùng qua @Async("aiTaskExecutor") trên phương thức inference.
 *
 * Thông số (bê từ monolith):
 *   corePoolSize  = 4  : luôn giữ 4 thread sẵn sàng (hợp server 4 vCPU).
 *   maxPoolSize   = 8  : burst tới 8 thread khi hàng đợi đầy.
 *   queueCapacity = 50 : tối đa 50 request chờ.
 *   Khi queue đầy và đã đạt maxPool: ném AI_SERVICE_BUSY (503) thay vì để
 *   RejectedExecutionException rơi xuống handler mặc định (500).
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "aiTaskExecutor")
    public Executor aiTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("ai-infer-");
        executor.setKeepAliveSeconds(60);

        // Queue đầy + đã đạt maxPool: log và ném AppException(503) ngay tại thread gọi (Tomcat)
        // → GlobalExceptionHandler trả 503. (Submit bị từ chối chạy đồng bộ ở thread submit.)
        executor.setRejectedExecutionHandler((runnable, pool) -> {
            log.error("AI task queue full (maxPool={}, queueCapacity=50). Request rejected.",
                    pool.getMaximumPoolSize());
            throw new AppException(ErrorCode.AI_SERVICE_BUSY);
        });

        executor.initialize();
        log.info("aiTaskExecutor initialized: core=4, max=8, queue=50");
        return executor;
    }
}
