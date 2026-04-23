package com.zokomart.backend.catalog.attribute.dto;

public record AdminAttributeDetailResponse(
        String id,
        String name,
        String code,
        String type,
        String categoryId,
        boolean filterable,
        boolean searchable,
        boolean required,
        boolean customAllowed,
        String status
) {
}
