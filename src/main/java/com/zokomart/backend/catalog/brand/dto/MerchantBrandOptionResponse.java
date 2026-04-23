package com.zokomart.backend.catalog.brand.dto;

public record MerchantBrandOptionResponse(
        String id,
        String name,
        String code,
        String status,
        String sourceType
) {
}
