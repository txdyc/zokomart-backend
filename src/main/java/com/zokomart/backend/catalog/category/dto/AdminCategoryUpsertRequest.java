package com.zokomart.backend.catalog.category.dto;

import jakarta.validation.constraints.NotBlank;

public record AdminCategoryUpsertRequest(
        @NotBlank String name,
        @NotBlank String code,
        String parentId,
        Integer sortOrder,
        String description
) {
}
