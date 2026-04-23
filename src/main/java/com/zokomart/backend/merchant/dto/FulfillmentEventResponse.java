package com.zokomart.backend.merchant.dto;

public record FulfillmentEventResponse(
        String status,
        String notes,
        String createdAt
) {
}
