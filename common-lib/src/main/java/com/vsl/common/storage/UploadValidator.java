package com.vsl.common.storage;

import com.vsl.common.exception.AppException;
import com.vsl.common.exception.ErrorCode;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.UUID;

/**
 * Kiểm tra file upload + sinh tên object an toàn.
 * Sửa lỗi bản monolith: (1) không kiểm loại file -> có thể đưa HTML/SVG vào bucket public (XSS);
 * (2) avatar dùng nguyên tên file của client trong tên object.
 * Giờ chỉ nhận các loại cho phép, đuôi file suy ra từ Content-Type, tên object = prefix/UUID.ext.
 */
public final class UploadValidator {

    public static final long MAX_IMAGE_BYTES = 5L * 1024 * 1024;   // 5 MB
    public static final long MAX_VIDEO_BYTES = 50L * 1024 * 1024;  // 50 MB (khớp giới hạn multipart)

    private static final Map<String, String> IMAGE_TYPES = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp",
            "image/gif", ".gif");

    private static final Map<String, String> VIDEO_TYPES = Map.of(
            "video/mp4", ".mp4",
            "video/webm", ".webm",
            "video/quicktime", ".mov",
            "video/x-msvideo", ".avi",
            "video/x-matroska", ".mkv");

    private UploadValidator() {
    }

    /** @return đuôi file (vd ".jpg") tương ứng loại ảnh hợp lệ */
    public static String requireImage(MultipartFile file) {
        return require(file, IMAGE_TYPES, MAX_IMAGE_BYTES, "ảnh (JPEG, PNG, WEBP, GIF)");
    }

    /** @return đuôi file (vd ".mp4") tương ứng loại video hợp lệ */
    public static String requireVideo(MultipartFile file) {
        return require(file, VIDEO_TYPES, MAX_VIDEO_BYTES, "video (MP4, WEBM, MOV, AVI, MKV)");
    }

    /** Tên object duy nhất, không chứa dữ liệu do client đặt: "{prefix}/{uuid}{ext}". */
    public static String objectName(String prefix, String extension) {
        return prefix + "/" + UUID.randomUUID() + extension;
    }

    private static String require(MultipartFile file, Map<String, String> allowed, long maxBytes, String label) {
        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_FILE, "Chưa chọn file hoặc file rỗng");
        }
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
        String ext = allowed.get(contentType);
        if (ext == null) {
            throw new AppException(ErrorCode.INVALID_FILE, "Chỉ chấp nhận " + label);
        }
        if (file.getSize() > maxBytes) {
            throw new AppException(ErrorCode.PAYLOAD_TOO_LARGE,
                    "File vượt quá " + (maxBytes / (1024 * 1024)) + " MB");
        }
        return ext;
    }
}
