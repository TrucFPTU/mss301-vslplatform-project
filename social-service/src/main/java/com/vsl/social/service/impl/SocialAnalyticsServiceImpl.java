package com.vsl.social.service.impl;

import com.vsl.social.dto.SocialDashboardOverviewResponse;
import com.vsl.social.dto.TopCreatorResponse;
import com.vsl.social.dto.TopPostResponse;
import com.vsl.social.entity.Blog;
import com.vsl.social.entity.BlogStatus;
import com.vsl.social.entity.ReportStatus;
import com.vsl.social.repository.BlogReportRepository;
import com.vsl.social.repository.BlogRepository;
import com.vsl.social.repository.projection.CreatorStatProjection;
import com.vsl.social.service.SocialAnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SocialAnalyticsServiceImpl implements SocialAnalyticsService {

    private final BlogRepository blogRepository;
    private final BlogReportRepository blogReportRepository;

    @Override
    public SocialDashboardOverviewResponse getOverview() {
        AccessControl.requireAdmin();

        long totalBlogs = blogRepository.count();
        long publishedBlogs = blogRepository.countByStatus(BlogStatus.PUBLISHED);
        long hiddenBlogs = blogRepository.countByStatus(BlogStatus.HIDDEN);
        long draftBlogs = blogRepository.countByStatus(BlogStatus.DRAFT);
        long totalLikes = blogRepository.sumLikeCount();
        long totalComments = blogRepository.sumCommentCount();
        long totalShares = blogRepository.sumShareCount();
        long pendingReports = blogReportRepository.countByStatus(ReportStatus.PENDING);
        long resolvedReports = blogReportRepository.countByStatus(ReportStatus.RESOLVED);
        long rejectedReports = blogReportRepository.countByStatus(ReportStatus.REJECTED);

        return SocialDashboardOverviewResponse.builder()
                .totalBlogs(totalBlogs)
                .publishedBlogs(publishedBlogs)
                .hiddenBlogs(hiddenBlogs)
                .draftBlogs(draftBlogs)
                .totalLikes(totalLikes)
                .totalComments(totalComments)
                .totalShares(totalShares)
                .pendingReports(pendingReports)
                .resolvedReports(resolvedReports)
                .rejectedReports(rejectedReports)
                .build();
    }

    @Override
    public List<TopPostResponse> getTopEngagedPosts(int limit) {
        AccessControl.requireAdmin();
        int safeLimit = Math.max(1, Math.min(limit, 50));
        return blogRepository.findTopEngagedBlogs(PageRequest.of(0, safeLimit)).stream()
                .map(TopPostResponse::from)
                .toList();
    }

    @Override
    public List<TopCreatorResponse> getTopCreators(int limit) {
        AccessControl.requireAdmin();
        int safeLimit = Math.max(1, Math.min(limit, 50));
        List<CreatorStatProjection> topCreators = blogRepository.findTopCreators(PageRequest.of(0, safeLimit));
        return topCreators.stream()
                .map(TopCreatorResponse::from)
                .toList();
    }
}