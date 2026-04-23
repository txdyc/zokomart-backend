package com.zokomart.backend.order.dto;

public record OrderItemResponse(
        String id,
        String productId,
        String skuId,
        String productName,
        String skuName,
        String unitPriceAmount,
        String currencyCode,
        int quantity,
        String lineTotalAmount
) {
}
