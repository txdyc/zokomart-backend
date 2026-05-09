package com.zokomart.backend.catalog.stats.dto;

public record TopCategoryResponse(
        String id,
        String categoryCode,
        String code,
        String name,
        String imageUrl,
        long viewCount
) {
}
