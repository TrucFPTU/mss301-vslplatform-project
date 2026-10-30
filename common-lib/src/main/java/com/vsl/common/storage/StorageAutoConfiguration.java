package com.vsl.common.storage;

import io.minio.MinioClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Bật StorageService khi service có thư viện MinIO trên classpath VÀ khai báo minio.endpoint.
 * Service không cần lưu file (ai-service) không bị ảnh hưởng.
 */
@AutoConfiguration
@ConditionalOnClass(MinioClient.class)
@ConditionalOnProperty(prefix = "minio", name = "endpoint")
@EnableConfigurationProperties(StorageProperties.class)
public class StorageAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public MinioClient minioClient(StorageProperties properties) {
        return MinioClient.builder()
                .endpoint(properties.getEndpoint())
                .credentials(properties.getAccessKey(), properties.getSecretKey())
                .build();
    }

    @Bean(initMethod = "initBuckets")
    @ConditionalOnMissingBean(StorageService.class)
    public MinioStorageService storageService(MinioClient minioClient, StorageProperties properties) {
        return new MinioStorageService(minioClient, properties);
    }
}
