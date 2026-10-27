package com.vsl.common.storage;

import java.io.File;
import java.io.InputStream;

/** Lưu file (ảnh, video) vào kho object (MinIO), trả URL công khai để frontend hiển thị. */
public interface StorageService {

    /** Upload luồng dữ liệu; trả URL công khai. Lỗi -> AppException(FILE_UPLOAD_ERROR). */
    String upload(String bucket, String objectName, InputStream data, long size, String contentType);

    /** Upload file trên đĩa (vd video đã chuyển mã); trả URL công khai. */
    String upload(String bucket, String objectName, File file, String contentType);

    /**
     * Xóa object dựa trên URL công khai đã lưu trong DB. Bỏ qua nếu url rỗng hoặc không thuộc bucket
     * này (vd URL ảnh ngoài). Lỗi khi xóa chỉ log, không ném (file mồ côi không làm hỏng nghiệp vụ).
     */
    void deleteByUrl(String bucket, String url);
}
