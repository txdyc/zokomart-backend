package com.zokomart.backend.admin.merchant.dto;

import java.util.Map;

public record AdminMerchantDetailResponse(
        Merchant merchant,
        Summary summary,
        Map<String, Long> orderStatusBreakdown
) {
    public record Merchant(
            String id,
            String merchantCode,
            String displayName,
            String merchantType,
            String status,
            long productCount,
            String createdAt,
            String updatedAt
    ) {
    }

    public record Summary(
            long orders7d,
            long orders30d,
            String gmv7d,
            String gmv30d,
            long pendingPaymentOrders,
            long pendingFulfillmentOrders,
            long cancelledOrders
    ) {
    }
}
