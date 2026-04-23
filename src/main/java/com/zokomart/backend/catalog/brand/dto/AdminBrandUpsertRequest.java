package com.zokomart.backend.catalog.brand.dto;

import jakarta.validation.constraints.NotBlank;

public record AdminBrandUpsertRequest(
        @NotBlank String name,
        @NotBlank String code
) {
}
