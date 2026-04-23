package com.zokomart.backend.order.dto;

public record PaymentIntentSummaryResponse(
        String id,
        String status,
        String amount,
        String currencyCode,
        String expiresAt
) {
}
