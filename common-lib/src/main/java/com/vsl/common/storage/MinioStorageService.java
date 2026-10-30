package com.vsl.common.storage;

import com.vsl.common.exception.AppException;
import com.vsl.common.exception.ErrorCode;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.SetBucketPolicyArgs;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

/** Bê từ MinioService của monolith, gom các hàm upload/xóa theo từng bucket thành dạng tổng quát. */
@Slf4j
public class MinioStorageService implements StorageService {

    private final MinioClient minioClient;
    private final StorageProperties properties;

    public MinioStorageService(MinioClient minioClient, StorageProperties properties) {
        this.minioClient = minioClient;
        this.properties = properties;
    }

    /**
     * Tạo các bucket của service nếu chưa có + đặt chính sách đọc công khai.
     * Lỗi MinIO KHÔNG chặn service khởi động (chỉ cảnh báo) - upload sẽ báo lỗi khi gọi.
     */
    public void initBuckets() {
        for (String bucket : properties.getBuckets()) {
            try {
                if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
                    minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                    log.info("MinIO: đã tạo bucket '{}'.", bucket);
                }
                // Luôn đặt lại policy để bucket tạo tay / cũ vẫn xem được qua URL công khai.
                minioClient.setBucketPolicy(SetBucketPolicyArgs.builder()
                        .bucket(bucket).config(publicReadPolicy(bucket)).build());
            } catch (Exception e) {
                log.warn("MinIO: không khởi tạo được bucket '{}' ({}). Upload sẽ lỗi cho tới khi MinIO sẵn sàng.",
                        bucket, e.getMessage());
            }
        }
    }

    @Override
    public String upload(String bucket, String objectName, InputStream data, long size, String contentType) {
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .stream(data, size, -1)
                    .contentType(contentType)
                    .build());
            String url = publicUrl(bucket, objectName);
            log.info("MinIO: đã upload {}/{}", bucket, objectName);
            return url;
        } catch (Exception e) {
            log.error("MinIO: upload {}/{} thất bại: {}", bucket, objectName, e.getMessage());
            throw new AppException(ErrorCode.FILE_UPLOAD_ERROR);
        }
    }

    @Override
    public String upload(String bucket, String objectName, File file, String contentType) {
        try (InputStream in = new FileInputStream(file)) {
            return upload(bucket, objectName, in, file.length(), contentType);
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("MinIO: không đọc được file {}: {}", file, e.getMessage());
            throw new AppException(ErrorCode.FILE_UPLOAD_ERROR);
        }
    }

    @Override
    public void deleteByUrl(String bucket, String url) {
        if (url == null || url.isBlank()) return;
        String marker = "/" + bucket + "/";
        int idx = url.indexOf(marker);
        if (idx < 0) return;
        String objectName = url.substring(idx + marker.length());
        try {
            minioClient.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(objectName).build());
            log.info("MinIO: đã xóa {}/{}", bucket, objectName);
        } catch (Exception e) {
            log.warn("MinIO: không xóa được {}/{}: {}", bucket, objectName, e.getMessage());
        }
    }

    private String publicUrl(String bucket, String objectName) {
        String base = properties.getPublicEndpoint() != null && !properties.getPublicEndpoint().isBlank()
                ? properties.getPublicEndpoint() : properties.getEndpoint();
        return base.replaceAll("/+$", "") + "/" + bucket + "/" + objectName;
    }

    private static String publicReadPolicy(String bucket) {
        return """
                {
                  "Version": "2012-10-17",
                  "Statement": [
                    {
                      "Effect": "Allow",
                      "Principal": {"AWS": ["*"]},
                      "Action": ["s3:GetObject"],
                      "Resource": ["arn:aws:s3:::%s/*"]
                    }
                  ]
                }
                """.formatted(bucket);
    }
}
