package com.zokomart.backend.admin.product.dto;

import java.util.List;

public record AdminProductListResponse(
        List<Item> items,
        int page,
        int pageSize,
        int total
) {
    public record Item(
            String id,
            String productCode,
            String name,
            String status,
            MerchantSummary merchant,
            CategorySummary category,
            String priceAmount,
            String currencyCode,
            boolean hasActiveSku,
            String thumbnailUrl,
            String updatedAt
    ) {
    }

    public record MerchantSummary(
            String id,
            String displayName
    ) {
    }

    public record CategorySummary(
            String id,
            String name
    ) {
    }
}
