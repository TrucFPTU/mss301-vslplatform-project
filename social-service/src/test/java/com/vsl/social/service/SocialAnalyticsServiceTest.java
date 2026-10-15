package com.vsl.social.service;

import com.vsl.common.exception.ErrorCode;
import com.vsl.social.dto.SocialDashboardOverviewResponse;
import com.vsl.social.dto.TopCreatorResponse;
import com.vsl.social.dto.TopPostResponse;
import com.vsl.social.entity.Blog;
import com.vsl.social.entity.BlogStatus;
import com.vsl.social.entity.ReportStatus;
import com.vsl.social.repository.BlogReportRepository;
import com.vsl.social.repository.BlogRepository;
import com.vsl.social.repository.projection.CreatorStatProjection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static com.vsl.social.service.TestSupport.ADMIN;
import static com.vsl.social.service.TestSupport.OTHER;
import static com.vsl.social.service.TestSupport.assertAppError;
import static com.vsl.social.service.TestSupport.loginAs;
import static com.vsl.social.service.TestSupport.logout;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SocialAnalyticsServiceTest {

    @Mock
    private BlogRepository blogRepository;

    @Mock
    private BlogReportRepository blogReportRepository;

    @InjectMocks
    private SocialAnalyticsService socialAnalyticsService;

    @AfterEach
    void tearDown() {
        logout();
    }

    @Test
    @DisplayName("getOverview -> Admin lấy thành công các chỉ số tổng quan mạng xã hội")
    void getOverview_Admin_Success() {
        loginAs(ADMIN, "ADMIN");

        when(blogRepository.count()).thenReturn(100L);
        when(blogRepository.countByStatus(BlogStatus.PUBLISHED)).thenReturn(80L);
        when(blogRepository.countByStatus(BlogStatus.HIDDEN)).thenReturn(5L);
        when(blogRepository.countByStatus(BlogStatus.DRAFT)).thenReturn(15L);

        when(blogRepository.sumLikeCount()).thenReturn(500L);
        when(blogRepository.sumCommentCount()).thenReturn(120L);
        when(blogRepository.sumShareCount()).thenReturn(45L);

        when(blogReportRepository.countByStatus(ReportStatus.PENDING)).thenReturn(3L);
        when(blogReportRepository.countByStatus(ReportStatus.RESOLVED)).thenReturn(10L);
        when(blogReportRepository.countByStatus(ReportStatus.REJECTED)).thenReturn(2L);

        SocialDashboardOverviewResponse overview = socialAnalyticsService.getOverview();

        assertThat(overview.totalBlogs()).isEqualTo(100L);
        assertThat(overview.publishedBlogs()).isEqualTo(80L);
        assertThat(overview.hiddenBlogs()).isEqualTo(5L);
        assertThat(overview.draftBlogs()).isEqualTo(15L);
        assertThat(overview.totalLikes()).isEqualTo(500L);
        assertThat(overview.totalComments()).isEqualTo(120L);
        assertThat(overview.totalShares()).isEqualTo(45L);
        assertThat(overview.pendingReports()).isEqualTo(3L);
        assertThat(overview.resolvedReports()).isEqualTo(10L);
        assertThat(overview.rejectedReports()).isEqualTo(2L);
    }

    @Test
    @DisplayName("getOverview -> Người dùng thường bị từ chối FORBIDDEN")
    void getOverview_NonAdmin_Forbidden() {
        loginAs(OTHER, "USER");
        assertAppError(() -> socialAnalyticsService.getOverview(), ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("getTopEngagedPosts -> Admin lấy top posts có tính totalInteractions chuẩn xác")
    void getTopEngagedPosts_Admin_Success() {
        loginAs(ADMIN, "ADMIN");

        Blog blog = Blog.builder()
                .authorId(10L)
                .title("Học ngôn ngữ ký hiệu chủ đề Gia đình")
                .status(BlogStatus.PUBLISHED)
                .likeCount(50L)
                .commentCount(20L)
                .shareCount(10L)
                .build();
        blog.setId(1L);

        when(blogRepository.findTopEngagedBlogs(any(Pageable.class))).thenReturn(List.of(blog));

        List<TopPostResponse> topPosts = socialAnalyticsService.getTopEngagedPosts(5);

        assertThat(topPosts).hasSize(1);
        TopPostResponse first = topPosts.get(0);
        assertThat(first.id()).isEqualTo(1L);
        assertThat(first.likeCount()).isEqualTo(50L);
        assertThat(first.commentCount()).isEqualTo(20L);
        assertThat(first.shareCount()).isEqualTo(10L);
        assertThat(first.totalInteractions()).isEqualTo(80L);
    }

    @Test
    @DisplayName("getTopEngagedPosts -> Non-Admin bị từ chối FORBIDDEN")
    void getTopEngagedPosts_NonAdmin_Forbidden() {
        loginAs(OTHER, "USER");
        assertAppError(() -> socialAnalyticsService.getTopEngagedPosts(5), ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("getTopCreators -> Admin lấy top creators thành công")
    void getTopCreators_Admin_Success() {
        loginAs(ADMIN, "ADMIN");

        CreatorStatProjection proj = mock(CreatorStatProjection.class);
        when(proj.getAuthorId()).thenReturn(100L);
        when(proj.getTotalPosts()).thenReturn(15L);
        when(proj.getPublishedPosts()).thenReturn(12L);
        when(proj.getTotalLikesReceived()).thenReturn(250L);

        when(blogRepository.findTopCreators(any(Pageable.class))).thenReturn(List.of(proj));

        List<TopCreatorResponse> topCreators = socialAnalyticsService.getTopCreators(5);

        assertThat(topCreators).hasSize(1);
        TopCreatorResponse creator = topCreators.get(0);
        assertThat(creator.authorId()).isEqualTo(100L);
        assertThat(creator.totalPosts()).isEqualTo(15L);
        assertThat(creator.publishedPosts()).isEqualTo(12L);
        assertThat(creator.totalLikesReceived()).isEqualTo(250L);
    }

    @Test
    @DisplayName("getTopCreators -> Non-Admin bị từ chối FORBIDDEN")
    void getTopCreators_NonAdmin_Forbidden() {
        loginAs(OTHER, "USER");
        assertAppError(() -> socialAnalyticsService.getTopCreators(5), ErrorCode.FORBIDDEN);
    }
}
