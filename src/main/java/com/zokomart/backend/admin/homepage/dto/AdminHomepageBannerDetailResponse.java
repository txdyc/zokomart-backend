package com.zokomart.backend.admin.homepage.dto;

import java.time.LocalDateTime;

public record AdminHomepageBannerDetailResponse(
        String id,
        String title,
        String imageUrl,
        String targetType,
        String targetProductId,
        String targetActivityKey,
        String targetLabel,
        int sortOrder,
        boolean enabled,
        boolean targetValid,
        LocalDateTime updatedAt
) {
}
