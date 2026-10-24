package com.vsl.social.service;

import com.vsl.social.dto.SocialDashboardOverviewResponse;
import com.vsl.social.dto.TopCreatorResponse;
import com.vsl.social.dto.TopPostResponse;

import java.util.List;

public interface SocialAnalyticsService {

    SocialDashboardOverviewResponse getOverview();

    List<TopPostResponse> getTopEngagedPosts(int limit);

    List<TopCreatorResponse> getTopCreators(int limit);
}
