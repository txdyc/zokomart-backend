package com.zokomart.backend.catalog.brand.dto;

public record AdminBrandDetailResponse(
        String id,
        String name,
        String code,
        String status,
        String sourceType,
        String imageUrl
) {
}
