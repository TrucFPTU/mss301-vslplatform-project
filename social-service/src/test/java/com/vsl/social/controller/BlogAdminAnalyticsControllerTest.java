package com.vsl.social.controller;

import com.vsl.social.dto.SocialDashboardOverviewResponse;
import com.vsl.social.dto.TopCreatorResponse;
import com.vsl.social.dto.TopPostResponse;
import com.vsl.social.entity.BlogStatus;
import com.vsl.social.service.SocialAnalyticsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class BlogAdminAnalyticsControllerTest {

    private MockMvc mockMvc;

    @Mock
    private SocialAnalyticsService socialAnalyticsService;

    @InjectMocks
    private BlogAdminAnalyticsController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("GET /api/blogs/admin/analytics/overview -> 200 OK và dữ liệu tổng quan")
    void getOverview_Returns200() throws Exception {
        SocialDashboardOverviewResponse overview = SocialDashboardOverviewResponse.builder()
                .totalBlogs(50L)
                .publishedBlogs(40L)
                .hiddenBlogs(2L)
                .draftBlogs(8L)
                .totalLikes(300L)
                .totalComments(75L)
                .totalShares(20L)
                .pendingReports(1L)
                .resolvedReports(5L)
                .rejectedReports(2L)
                .build();

        when(socialAnalyticsService.getOverview()).thenReturn(overview);

        mockMvc.perform(get("/api/blogs/admin/analytics/overview")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.totalBlogs").value(50L))
                .andExpect(jsonPath("$.data.publishedBlogs").value(40L))
                .andExpect(jsonPath("$.data.totalLikes").value(300L))
                .andExpect(jsonPath("$.data.pendingReports").value(1L));

        verify(socialAnalyticsService).getOverview();
    }

    @Test
    @DisplayName("GET /api/blogs/admin/analytics/top-posts -> 200 OK và danh sách top posts")
    void getTopEngagedPosts_Returns200() throws Exception {
        TopPostResponse post = TopPostResponse.builder()
                .id(1L)
                .authorId(10L)
                .title("Bài viết nổi bật")
                .status(BlogStatus.PUBLISHED)
                .likeCount(100L)
                .commentCount(25L)
                .shareCount(15L)
                .totalInteractions(140L)
                .createdAt(Instant.now())
                .build();

        when(socialAnalyticsService.getTopEngagedPosts(eq(10))).thenReturn(List.of(post));

        mockMvc.perform(get("/api/blogs/admin/analytics/top-posts")
                        .param("limit", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data[0].id").value(1L))
                .andExpect(jsonPath("$.data[0].totalInteractions").value(140L));

        verify(socialAnalyticsService).getTopEngagedPosts(eq(10));
    }

    @Test
    @DisplayName("GET /api/blogs/admin/analytics/top-creators -> 200 OK và danh sách top creators")
    void getTopCreators_Returns200() throws Exception {
        TopCreatorResponse creator = TopCreatorResponse.builder()
                .authorId(100L)
                .totalPosts(10L)
                .publishedPosts(8L)
                .totalLikesReceived(150L)
                .build();

        when(socialAnalyticsService.getTopCreators(eq(5))).thenReturn(List.of(creator));

        mockMvc.perform(get("/api/blogs/admin/analytics/top-creators")
                        .param("limit", "5")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data[0].authorId").value(100L))
                .andExpect(jsonPath("$.data[0].totalPosts").value(10L));

        verify(socialAnalyticsService).getTopCreators(eq(5));
    }

    @Test
    @DisplayName("GET /api/admin/social/analytics/overview -> route alias hoạt động bình thường")
    void aliasRoute_WorksProperly() throws Exception {
        SocialDashboardOverviewResponse overview = SocialDashboardOverviewResponse.builder().build();
        when(socialAnalyticsService.getOverview()).thenReturn(overview);

        mockMvc.perform(get("/api/admin/social/analytics/overview")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"));
    }
}
