package com.zokomart.backend.admin.category.dto;

import java.util.List;

public record AdminCategoryListResponse(
        List<Item> items,
        int page,
        int pageSize,
        int total
) {
    public record Item(
            String id,
            String categoryCode,
            String name,
            String status,
            long productCount,
            String imageUrl,
            String updatedAt
    ) {
    }
}
