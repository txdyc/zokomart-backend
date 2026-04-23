package com.zokomart.backend.merchantadmin.product.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record MerchantAdminProductUpsertRequest(
        @NotBlank String merchantId,
        String productCode,
        @NotBlank String name,
        String description,
        String descriptionHtml,
        @NotBlank String categoryId,
        String brandId,
        List<@Valid AttributeValueRequest> attributes,
        @NotEmpty List<@Valid SkuRequest> skus
) {
    public String resolvedDescriptionHtml() {
        return descriptionHtml != null && !descriptionHtml.isBlank() ? descriptionHtml : description;
    }

    public String resolvedProductCode() {
        return productCode != null && !productCode.isBlank()
                ? productCode.trim()
                : "PRD-" + UUID.randomUUID().toString().replace("-", "").toUpperCase();
    }

    public List<AttributeValueRequest> safeAttributes() {
        return attributes == null ? List.of() : attributes;
    }

    public record AttributeValueRequest(
            String attributeId,
            String attributeCode,
            String customAttributeName,
            String type,
            String valueText,
            BigDecimal valueNumber,
            Boolean valueBoolean,
            JsonNode valueJson
    ) {
    }

    public record SkuRequest(
            @NotBlank String skuCode,
            String skuName,
            String attributesJson,
            JsonNode specsJson,
            BigDecimal unitPriceAmount,
            BigDecimal price,
            BigDecimal originalPrice,
            BigDecimal costPrice,
            String currencyCode,
            Integer availableQuantity,
            Integer stock
    ) {
        public BigDecimal resolvedPrice() {
            return price != null ? price : unitPriceAmount;
        }

        public BigDecimal resolvedOriginalPrice() {
            return originalPrice != null ? originalPrice : resolvedPrice();
        }

        public BigDecimal resolvedCostPrice() {
            return costPrice != null ? costPrice : resolvedPrice();
        }

        public int resolvedStock() {
            if (stock != null) {
                return stock;
            }
            return availableQuantity == null ? 0 : availableQuantity;
        }

        public String resolvedCurrencyCode() {
            return currencyCode == null || currencyCode.isBlank() ? "GHS" : currencyCode.trim();
        }
    }
}
