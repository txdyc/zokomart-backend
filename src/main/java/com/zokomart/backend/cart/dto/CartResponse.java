package com.zokomart.backend.cart.dto;

import java.util.List;

public record CartResponse(
        String id,
        String buyerId,
        String merchantId,
        List<CartItemResponse> items,
        String currencyCode,
        String referenceTotalAmount
) {
}
