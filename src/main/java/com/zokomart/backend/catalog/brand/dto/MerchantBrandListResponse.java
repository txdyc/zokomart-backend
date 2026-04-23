package com.zokomart.backend.catalog.brand.dto;

import java.util.List;

public record MerchantBrandListResponse(
        List<MerchantBrandOptionResponse> items
) {
}
