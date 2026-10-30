package com.vsl.social.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestClient;

/**
 * Hai RestClient.Builder (giống learning-service):
 *  - @Primary, KHÔNG load-balanced: cho hạ tầng (Eureka client) tự dùng.
 *  - @LoadBalanced: gọi service khác bằng tên Eureka (http://identity-service, http://ai-service).
 */
@Configuration
public class RestClientConfig {

    @Bean
    @Primary
    RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    @LoadBalanced
    RestClient.Builder loadBalancedRestClientBuilder() {
        return RestClient.builder();
    }
}
