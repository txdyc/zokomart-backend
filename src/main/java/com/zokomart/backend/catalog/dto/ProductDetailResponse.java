package com.zokomart.backend.catalog.dto;

import java.util.List;

public record ProductDetailResponse(
        String id,
        String name,
        String description,
        String merchantId,
        String merchantName,
        String merchantType,
        String status,
        String primaryImageUrl,
        String defaultSkuId,
        PriceRange priceRange,
        List<OptionGroup> optionGroups,
        List<ProductSkuResponse> skus
) {
    public record PriceRange(
            String minPriceAmount,
            String maxPriceAmount,
            String currencyCode
    ) {
    }

    public record OptionGroup(
            String code,
            String label,
            List<OptionValue> values
    ) {
    }

    public record OptionValue(
            String value,
            String label
    ) {
    }
}
