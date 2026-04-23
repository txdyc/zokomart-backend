package com.zokomart.backend.merchantadmin.product.dto;

import jakarta.validation.constraints.NotBlank;

public record MerchantAdminProductDeactivateRequest(@NotBlank String merchantId) {
}
