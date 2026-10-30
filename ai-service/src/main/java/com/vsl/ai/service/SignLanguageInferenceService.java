package com.vsl.ai.service;

import com.vsl.ai.dto.EvaluationResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

/**
 * Chấm điểm video ký hiệu bằng model ONNX (STATELESS - KHÔNG ghi DB).
 *
 * <p>Controller gọi qua interface này (theo quy ước team: tầng service = interface + Impl,
 * controller KHÔNG phụ thuộc trực tiếp vào lớp Impl). Hiện có 1 hiện thực:
 * {@link SignLanguageInferenceServiceImpl} (ONNX Runtime + JavaCV).
 */
public interface SignLanguageInferenceService {

    /**
     * Chấm điểm 1 video luyện tập so với từ mong đợi.
     *
     * @param videoFile  video người học quay (multipart).
     * @param expectedId class ID của từ cần luyện (0-999).
     * @param startFrac  mốc bắt đầu cửa sổ động tác trong video (0.0-1.0).
     * @param endFrac    mốc kết thúc cửa sổ động tác trong video (0.0-1.0).
     * @param validIds   các expectedId của từ CÓ VIDEO mẫu (learning-service truyền sang) để
     *                   softmax MASKED. Rỗng/null → fallback softmax trên cả 1000 lớp.
     * @return kết quả chấm điểm (status/confidence/predictedId/rank), bất đồng bộ.
     */
    CompletableFuture<EvaluationResponse> evaluate(MultipartFile videoFile, int expectedId,
                                                   float startFrac, float endFrac,
                                                   Collection<Integer> validIds);
}
