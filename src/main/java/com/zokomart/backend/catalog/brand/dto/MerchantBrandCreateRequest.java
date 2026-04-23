package com.zokomart.backend.catalog.brand.dto;

import jakarta.validation.constraints.NotBlank;

public record MerchantBrandCreateRequest(
        @NotBlank String merchantId,
        @NotBlank String name,
        @NotBlank String code
) {
}
