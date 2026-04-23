package com.zokomart.backend.merchant.dto;

public record MerchantOrderItemResponse(
        String productName,
        String skuName,
        int quantity
) {
}
