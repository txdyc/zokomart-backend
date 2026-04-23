package com.zokomart.backend.admin.merchant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminMerchantCreateRequest(
        @Size(max = 64)
        @NotBlank String merchantCode,
        @Size(max = 160)
        @NotBlank String displayName,
        @Size(max = 32)
        @NotBlank String merchantType
) {
}
