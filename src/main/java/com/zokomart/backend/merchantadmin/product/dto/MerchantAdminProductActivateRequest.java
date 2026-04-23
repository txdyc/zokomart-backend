package com.zokomart.backend.merchantadmin.product.dto;

import jakarta.validation.constraints.NotBlank;

public record MerchantAdminProductActivateRequest(@NotBlank String merchantId) {
}
