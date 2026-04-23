package com.zokomart.backend.cart.dto;

public record CartItemResponse(
        String id,
        String productId,
        String skuId,
        String productName,
        String skuName,
        String referencePriceAmount,
        String currencyCode,
        int quantity,
        String lineReferenceTotal
) {
}
