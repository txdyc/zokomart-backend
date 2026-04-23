package com.zokomart.backend.catalog.banner.dto;

public record HomeBannerResponse(
        String id,
        String title,
        String imageUrl,
        String targetType,
        String targetHref
) {
}
