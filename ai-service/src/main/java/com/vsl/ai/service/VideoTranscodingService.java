package com.vsl.ai.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.concurrent.CompletableFuture;

/**
 * Chuẩn hóa video về H.264 + AAC trong MP4 "faststart" để mọi trình duyệt phát được qua thẻ video.
 * Bê từ VideoTranscodingService của monolith; đặt ở ai-service vì đây là nơi duy nhất có JavaCV/FFmpeg.
 */
public interface VideoTranscodingService {

    /** @return nội dung file MP4 đã chuyển mã (bất đồng bộ trên aiTaskExecutor) */
    CompletableFuture<byte[]> transcodeToH264(MultipartFile video);
}
