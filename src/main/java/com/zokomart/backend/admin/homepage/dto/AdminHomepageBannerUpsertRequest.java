package com.zokomart.backend.admin.homepage.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AdminHomepageBannerUpsertRequest(
        @NotBlank String title,
        @NotBlank String targetType,
        String targetProductId,
        String targetActivityKey,
        @NotNull @Min(0) Integer sortOrder,
        @NotNull Boolean enabled
) {
}
