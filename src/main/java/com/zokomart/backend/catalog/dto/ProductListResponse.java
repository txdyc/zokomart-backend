package com.zokomart.backend.catalog.dto;

import java.util.List;

public record ProductListResponse(
        List<ProductListItemResponse> items,
        int page,
        int pageSize,
        int total
) {
}
