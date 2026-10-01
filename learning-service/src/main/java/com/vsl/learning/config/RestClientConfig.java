package com.vsl.learning.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestClient;

/**
 * Hai RestClient.Builder:
 *  - restClientBuilder (@Primary, KHÔNG load-balanced): cho hạ tầng như Eureka client tự dùng.
 *    Nếu builder duy nhất là @LoadBalanced, Eureka client sẽ hiểu "eureka-server" là tên service
 *    và không đăng ký được.
 *  - loadBalancedRestClientBuilder (@LoadBalanced): gọi service khác bằng TÊN trên Eureka
 *    (vd http://ai-service/...), inject bằng qualifier @LoadBalanced.
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
