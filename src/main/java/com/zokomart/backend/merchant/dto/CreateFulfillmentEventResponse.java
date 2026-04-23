package com.zokomart.backend.merchant.dto;

public record CreateFulfillmentEventResponse(
        String orderId,
        FulfillmentStatusResponse fulfillment,
        FulfillmentEventResponse event
) {
}
