package com.zokomart.backend.merchant.dto;

import java.util.List;

public record MerchantOrderDetailResponse(
        String id,
        String orderNumber,
        String status,
        List<MerchantOrderItemResponse> items,
        MerchantShippingAddressResponse shippingAddress,
        MerchantFulfillmentDetailResponse fulfillment
) {
}
