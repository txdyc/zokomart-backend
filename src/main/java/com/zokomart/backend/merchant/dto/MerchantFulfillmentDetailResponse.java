package com.zokomart.backend.merchant.dto;

import java.util.List;

public record MerchantFulfillmentDetailResponse(
        String status,
        List<FulfillmentEventResponse> events
) {
}
