package com.vsl.learning.service;

import com.vsl.learning.dto.ai.EvaluationResponse;
import com.vsl.learning.dto.response.PracticeStatsResponse;
import com.vsl.learning.dto.response.UserProgressResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface PracticeService {

    /** Không đặt @Transactional: tránh giữ kết nối DB trong lúc chờ ai-service suy luận. */
    EvaluationResponse evaluate(Long userId, MultipartFile video, int expectedId,
                                       float startFrac, float endFrac);

    List<UserProgressResponse> getMyProgress(Long userId);

    PracticeStatsResponse getStats(Long userId);
}
