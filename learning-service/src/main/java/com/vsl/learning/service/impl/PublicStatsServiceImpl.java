package com.vsl.learning.service.impl;

import com.vsl.learning.client.IdentityServiceClient;
import com.vsl.learning.dto.remote.RemoteUserStats;
import com.vsl.learning.dto.response.PublicStatsResponse;
import com.vsl.learning.repository.VocabularyRepository;
import com.vsl.learning.service.PublicStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Bê từ monolith. Tổng người dùng nằm ở identity-service nên gọi sang qua Eureka. */
@Service
@RequiredArgsConstructor
public class PublicStatsServiceImpl implements PublicStatsService {

    /** Hằng số hiển thị trên landing page (giữ nguyên bản gốc). */
    private static final int SATISFACTION_RATE = 98;

    private final VocabularyRepository vocabularyRepository;
    private final IdentityServiceClient identityServiceClient;

    @Override
    @Transactional(readOnly = true)
    public PublicStatsResponse getLandingStats() {
        RemoteUserStats userStats = identityServiceClient.getUserStats();
        return new PublicStatsResponse(
                userStats != null ? userStats.totalUsers() : null,
                vocabularyRepository.count(),
                SATISFACTION_RATE);
    }
}
