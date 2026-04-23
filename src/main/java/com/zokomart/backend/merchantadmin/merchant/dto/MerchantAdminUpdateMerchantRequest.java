package com.zokomart.backend.merchantadmin.merchant.dto;

import jakarta.validation.constraints.NotBlank;

public record MerchantAdminUpdateMerchantRequest(@NotBlank String displayName) {
}
