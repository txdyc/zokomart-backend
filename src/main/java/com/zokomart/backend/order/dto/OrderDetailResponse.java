package com.zokomart.backend.order.dto;

import java.util.List;

public record OrderDetailResponse(
        String id,
        String orderNumber,
        String buyerId,
        String merchantId,
        String status,
        String currencyCode,
        String totalAmount,
        List<OrderItemResponse> items,
        PaymentIntentSummaryResponse paymentIntent,
        ShippingAddressResponse shippingAddress,
        String createdAt
) {
}
