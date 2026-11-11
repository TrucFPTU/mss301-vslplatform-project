package com.vsl.common.openapi;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import java.util.List;

/**
 * Tự cấu hình Swagger/OpenAPI cho mọi service dùng common-lib:
 *   - Tiêu đề trang lấy theo tên service (spring.application.name).
 *   - Thêm sẵn cơ chế bảo mật "Bearer JWT" → hiện nút "Authorize" trên Swagger UI,
 *     dán access token vào là gọi được các API cần đăng nhập.
 *
 * Mỗi service tự có tại:  http://<host>:<port>/swagger-ui.html
 * Trang chung gom 4 service (dropdown): http://<host>:8080/swagger-ui.html (api-gateway).
 */
@AutoConfiguration
@ConditionalOnClass(OpenAPI.class)
public class OpenApiAutoConfiguration {

    private static final String SCHEME_NAME = "bearerAuth";

    @Bean
    @ConditionalOnMissingBean
    public OpenAPI vslOpenAPI(@Value("${spring.application.name:VSL Service}") String appName) {
        return new OpenAPI()
                .info(new Info()
                        .title(appName + " API")
                        .version("v1")
                        .description("VSL Platform — tài liệu API cho " + appName))
                // Server tương đối "/": Swagger UI gọi API về chính nơi đang mở trang - cổng riêng của service
                // hoặc gateway :8080 (trang Swagger chung gom 4 service đọc tài liệu qua /api-docs/<service>).
                .servers(List.of(new Server().url("/")))
                .addSecurityItem(new SecurityRequirement().addList(SCHEME_NAME))
                .components(new Components().addSecuritySchemes(SCHEME_NAME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
