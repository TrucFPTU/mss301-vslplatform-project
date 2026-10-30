package com.vsl.learning.service.impl;

import com.vsl.common.exception.AppException;
import com.vsl.common.exception.ErrorCode;
import com.vsl.common.storage.StorageService;
import com.vsl.common.storage.UploadValidator;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Upload ảnh / video của learning-service lên MinIO (bucket giữ tên như monolith).
 * File cũ chỉ bị xóa SAU KHI transaction commit - rollback thì DB không trỏ vào file đã mất.
 */
@Component
public class MediaStorage {

    public static final String TUTORIAL_BUCKET = "tutorial-videos";
    public static final String VOCABULARY_IMAGE_BUCKET = "vocabulary-images";
    public static final String CATEGORY_IMAGE_BUCKET = "category-images";

    private final ObjectProvider<StorageService> storage;

    public MediaStorage(ObjectProvider<StorageService> storage) {
        this.storage = storage;
    }

    /** Kiểm tra là ảnh hợp lệ rồi upload; trả URL công khai. */
    public String uploadImage(String bucket, String prefix, MultipartFile image) {
        String ext = UploadValidator.requireImage(image);
        try (InputStream in = image.getInputStream()) {
            return storage().upload(bucket, UploadValidator.objectName(prefix, ext), in, image.getSize(), image.getContentType());
        } catch (IOException e) {
            throw new AppException(ErrorCode.INVALID_FILE, "Không đọc được file ảnh");
        }
    }

    /** Upload video đã chuyển mã (MP4). */
    public String uploadMp4(String bucket, String prefix, byte[] mp4) {
        return storage().upload(bucket, UploadValidator.objectName(prefix, ".mp4"),
                new ByteArrayInputStream(mp4), mp4.length, "video/mp4");
    }

    public void deleteAfterCommit(String bucket, String url) {
        StorageService s = storage.getIfAvailable();
        if (s == null || url == null || url.isBlank()) {
            return;
        }
        Runnable delete = () -> s.deleteByUrl(bucket, url);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    delete.run();
                }
            });
        } else {
            delete.run();
        }
    }

    private StorageService storage() {
        StorageService s = storage.getIfAvailable();
        if (s == null) {
            throw new AppException(ErrorCode.FILE_UPLOAD_ERROR, "Chưa cấu hình kho lưu trữ (MinIO)");
        }
        return s;
    }
}
