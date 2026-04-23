package com.zokomart.backend.catalog.dto;

import java.util.List;

public record ProductSkuResponse(
        String id,
        String skuCode,
        String skuName,
        String priceAmount,
        String originalPriceAmount,
        String currencyCode,
        int availableQuantity,
        boolean inStock,
        List<OptionValue> optionValues
) {
    public record OptionValue(
            String optionCode,
            String optionValue
    ) {
    }
}
