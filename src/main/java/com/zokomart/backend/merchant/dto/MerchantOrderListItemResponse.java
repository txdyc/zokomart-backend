package com.zokomart.backend.merchant.dto;

public record MerchantOrderListItemResponse(
        String id,
        String orderNumber,
        String merchantId,
        String status,
        String buyerDisplayName,
        String totalAmount,
        String currencyCode,
        String createdAt
) {
}
