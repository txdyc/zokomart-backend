package com.zokomart.backend.admin.product.dto;

import java.util.List;

public record AdminProductDetailResponse(
        String id,
        String productCode,
        String name,
        String description,
        String status,
        MerchantDetail merchant,
        CategoryDetail category,
        String thumbnailUrl,
        List<ImageItem> images,
        List<SkuItem> skus
) {
    public record MerchantDetail(
            String id,
            String displayName,
            String merchantType,
            String status
    ) {
    }

    public record CategoryDetail(
            String id,
            String name,
            String status
    ) {
    }

    public record SkuItem(
            String id,
            String skuCode,
            String skuName,
            String priceAmount,
            String currencyCode,
            int availableQuantity,
            String status
    ) {
    }

    public record ImageItem(
            String id,
            String imageUrl,
            int sortOrder,
            boolean isPrimary
    ) {
    }
}
