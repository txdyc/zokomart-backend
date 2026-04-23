package com.zokomart.backend.admin.order.dto;

import java.util.List;

public record AdminOrderListResponse(
        List<Item> items,
        int page,
        int pageSize,
        int total
) {
    public record Item(
            String id,
            String orderNumber,
            String buyerId,
            String merchantId,
            String status,
            String totalAmount,
            String currencyCode,
            PaymentIntent paymentIntent,
            String createdAt
    ) {
    }

    public record PaymentIntent(
            String status,
            String expiresAt
    ) {
    }
}
