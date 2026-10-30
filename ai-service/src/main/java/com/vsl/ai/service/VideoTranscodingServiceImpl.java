package com.vsl.ai.service;

import com.vsl.common.exception.AppException;
import com.vsl.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.util.concurrent.CompletableFuture;

/**
 * Lý do cần chuyển mã (từ monolith): video nguồn hay được mã hóa bằng codec trình duyệt không giải mã được
 * (vd MPEG-4 Part 2 "mp4v"), khiến video hướng dẫn đứng hình trên UI. H.264 + AAC phát được ở mọi trình duyệt.
 * Sửa so với bản cũ: làm tròn kích thước về số chẵn (yuv420p bắt buộc), video kích thước lẻ không còn lỗi.
 */
@Slf4j
@Service
public class VideoTranscodingServiceImpl implements VideoTranscodingService {

    private static final double DEFAULT_FRAME_RATE = 25.0;
    private static final int VIDEO_BITRATE = 2_000_000;
    private static final int AUDIO_BITRATE = 128_000;

    @Override
    @Async("aiTaskExecutor")
    public CompletableFuture<byte[]> transcodeToH264(MultipartFile video) {
        if (video == null || video.isEmpty()) {
            throw new AppException(ErrorCode.VIDEO_EMPTY);
        }
        File input = null;
        File output = null;
        try {
            input = File.createTempFile("vsl_transcode_in_", ".bin");
            video.transferTo(input);
            output = File.createTempFile("vsl_transcode_out_", ".mp4");
            transcode(input, output);
            byte[] bytes = Files.readAllBytes(output.toPath());
            log.info("Đã chuyển mã video {} KB -> {} KB", input.length() / 1024, bytes.length / 1024);
            return CompletableFuture.completedFuture(bytes);
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            throw new AppException(ErrorCode.VIDEO_CORRUPT, "Lỗi khi chuyển mã video: " + e.getMessage());
        } finally {
            deleteQuietly(input);
            deleteQuietly(output);
        }
    }

    private void transcode(File input, File output) throws Exception {
        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(input)) {
            try {
                grabber.start();
            } catch (Exception e) {
                throw new AppException(ErrorCode.VIDEO_CORRUPT, "Không đọc được video nguồn: " + e.getMessage());
            }
            int width = grabber.getImageWidth() & ~1;   // yuv420p cần kích thước chẵn
            int height = grabber.getImageHeight() & ~1;
            if (width <= 0 || height <= 0) {
                throw new AppException(ErrorCode.VIDEO_NO_FRAMES);
            }
            int audioChannels = Math.max(grabber.getAudioChannels(), 0);
            double frameRate = grabber.getFrameRate() > 0 ? grabber.getFrameRate() : DEFAULT_FRAME_RATE;

            try (FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(output, width, height, audioChannels)) {
                recorder.setFormat("mp4");
                recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
                recorder.setPixelFormat(avutil.AV_PIX_FMT_YUV420P);
                recorder.setFrameRate(frameRate);
                recorder.setVideoBitrate(VIDEO_BITRATE);
                // moov atom ở đầu file -> trình duyệt phát ngay, không cần tải hết.
                recorder.setOption("movflags", "faststart");
                if (audioChannels > 0) {
                    recorder.setAudioCodec(avcodec.AV_CODEC_ID_AAC);
                    recorder.setAudioBitrate(AUDIO_BITRATE);
                    recorder.setSampleRate(grabber.getSampleRate());
                }
                recorder.start();
                int videoFrames = 0;
                Frame frame;
                while ((frame = grabber.grab()) != null) {
                    if (frame.image != null) videoFrames++;
                    recorder.record(frame);
                }
                recorder.stop();
                if (videoFrames == 0) {
                    throw new AppException(ErrorCode.VIDEO_NO_FRAMES);
                }
            }
            grabber.stop();
        }
    }

    private static void deleteQuietly(File f) {
        if (f != null && f.exists() && !f.delete()) {
            f.deleteOnExit();
        }
    }
}
