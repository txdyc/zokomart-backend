package com.zokomart.backend.catalog.attribute.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AdminAttributeUpsertRequest(
        @NotBlank String name,
        @NotBlank String code,
        @NotBlank String type,
        @NotBlank String categoryId,
        @NotNull Boolean filterable,
        @NotNull Boolean searchable,
        @NotNull Boolean required,
        @NotNull Boolean customAllowed
) {
}
