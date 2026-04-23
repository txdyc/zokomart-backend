package com.zokomart.backend.admin.merchant.dto;

import java.util.List;

public record AdminMerchantListResponse(
        List<Item> items,
        int page,
        int pageSize,
        int total
) {
    public record Item(
            String id,
            String merchantCode,
            String displayName,
            String merchantType,
            String status,
            long productCount,
            String createdAt,
            String lastOrderAt,
            long pendingPaymentOrderCount,
            long pendingFulfillmentOrderCount
    ) {
    }
}
