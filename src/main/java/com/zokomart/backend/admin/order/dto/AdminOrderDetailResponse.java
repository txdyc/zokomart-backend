package com.zokomart.backend.admin.order.dto;

import java.util.List;

public record AdminOrderDetailResponse(
        String id,
        String orderNumber,
        String buyerId,
        String merchantId,
        String status,
        String totalAmount,
        String currencyCode,
        List<Item> items,
        PaymentIntent paymentIntent,
        ShippingAddress shippingAddress,
        String createdAt
) {
    public record Item(
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

    public record PaymentIntent(
            String status,
            String expiresAt
    ) {
    }

    public record ShippingAddress(
            String recipientName,
            String phoneNumber,
            String addressLine1,
            String addressLine2,
            String city,
            String region,
            String countryCode
    ) {
    }
}
