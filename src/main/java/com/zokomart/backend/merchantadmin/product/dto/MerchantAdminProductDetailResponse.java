package com.zokomart.backend.merchantadmin.product.dto;

import java.util.List;

public record MerchantAdminProductDetailResponse(
        String id,
        String productCode,
        String name,
        String description,
        String status,
        MerchantSummary merchant,
        CategorySummary category,
        SpuSummary spu,
        List<ImageItem> images,
        List<AttributeItem> attributes,
        List<SkuItem> skus
) {
    public record MerchantSummary(String id, String displayName) {
    }

    public record CategorySummary(String id, String name) {
    }

    public record SpuSummary(
            String id,
            String name,
            String brandId,
            String descriptionHtml,
            String categoryId,
            String status
    ) {
    }

    public record AttributeItem(
            String attributeId,
            String attributeCode,
            String customAttributeName,
            String valueText
    ) {
    }

    public record SkuItem(
            String id,
            String skuCode,
            String skuName,
            String priceAmount,
            String currencyCode,
            int availableQuantity,
            String specsJson,
            String price,
            String originalPrice,
            String costPrice,
            int stock,
            int lockedStock,
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
