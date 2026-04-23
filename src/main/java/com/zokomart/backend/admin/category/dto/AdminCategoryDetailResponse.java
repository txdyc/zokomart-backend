package com.zokomart.backend.admin.category.dto;

public record AdminCategoryDetailResponse(
        String id,
        String categoryCode,
        String name,
        String description,
        String status,
        long productCount,
        String imageUrl,
        String updatedAt
) {
    public AdminCategoryDetailResponse(
            String id,
            String categoryCode,
            String name,
            String description,
            String status,
            long productCount,
            String updatedAt
    ) {
        this(id, categoryCode, name, description, status, productCount, null, updatedAt);
    }
}
