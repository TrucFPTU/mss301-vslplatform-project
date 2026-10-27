package com.vsl.common.storage;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Cấu hình MinIO (prefix "minio"). Service nào cần lưu file mới khai báo.
 * <pre>
 * minio:
 *   endpoint: http://minio:9000           # địa chỉ service gọi tới (trong mạng Docker)
 *   public-endpoint: http://localhost:9000 # địa chỉ trình duyệt dùng để xem file
 *   access-key: minioadmin
 *   secret-key: minioadmin
 *   buckets: [avatars]                     # bucket service này sở hữu, tự tạo + public-read khi khởi động
 * </pre>
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "minio")
public class StorageProperties {
    private String endpoint;
    private String publicEndpoint;
    private String accessKey;
    private String secretKey;
    private List<String> buckets = new ArrayList<>();
}
