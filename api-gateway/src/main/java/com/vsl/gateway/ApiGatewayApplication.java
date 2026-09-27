package com.vsl.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * API Gateway — cổng vào DUY NHẤT của hệ thống (cổng 8080).
 * Client chỉ nói chuyện với gateway; gateway định tuyến tới đúng service
 * qua tên đăng ký trên Eureka (uri dạng lb://ten-service).
 */
@SpringBootApplication
public class ApiGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
