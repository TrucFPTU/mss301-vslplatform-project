package com.vsl.identity.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Bật @Scheduled cho identity: relay của outbox (saga xóa tài khoản, {@code OutboxRelayServiceImpl}). */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
