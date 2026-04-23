package com.zokomart.backend.merchant.dto;

import java.util.List;

public record MerchantOrderListResponse(
        List<MerchantOrderListItemResponse> items
) {
}
