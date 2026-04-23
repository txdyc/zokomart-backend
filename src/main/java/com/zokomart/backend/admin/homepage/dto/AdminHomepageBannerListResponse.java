package com.zokomart.backend.admin.homepage.dto;

import java.util.List;

public record AdminHomepageBannerListResponse(
        List<AdminHomepageBannerDetailResponse> items
) {
}
