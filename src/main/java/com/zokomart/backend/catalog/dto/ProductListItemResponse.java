package com.zokomart.backend.catalog.dto;

public record ProductListItemResponse(
        String id,
        String name,
        String merchantId,
        String merchantName,
        String merchantType,
        String priceAmount,
        String currencyCode,
        boolean inStock,
        String thumbnailUrl
) {
}
