package com.vsl.ai.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestClient;

/**
 * Hai RestClient.Builder (giống learning-service):
 *  - restClientBuilder (@Primary, KHÔNG load-balanced): cho hạ tầng như Eureka client tự dùng.
 *  - loadBalancedRestClientBuilder (@LoadBalanced): gọi service khác bằng TÊN trên Eureka
 *    (vd http://learning-service/...), inject bằng qualifier @LoadBalanced.
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
