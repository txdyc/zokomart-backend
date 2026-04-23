package com.zokomart.backend.catalog.attribute.dto;

import java.util.List;

public record AdminAttributeListResponse(
        String categoryId,
        List<AdminAttributeDetailResponse> items
) {
}
