package com.vsl.common.openapi;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Tự cấu hình Swagger/OpenAPI cho mọi service dùng common-lib:
 *   - Tiêu đề trang lấy theo tên service (spring.application.name).
 *   - Thêm sẵn cơ chế bảo mật "Bearer JWT" → hiện nút "Authorize" trên Swagger UI,
 *     dán access token vào là gọi được các API cần đăng nhập.
 *
 * Mỗi service tự có tại:  http://<host>:<port>/swagger-ui.html
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
                .addSecurityItem(new SecurityRequirement().addList(SCHEME_NAME))
                .components(new Components().addSecuritySchemes(SCHEME_NAME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
