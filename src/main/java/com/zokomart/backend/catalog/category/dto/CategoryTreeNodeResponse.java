package com.zokomart.backend.catalog.category.dto;

import java.util.List;

public record CategoryTreeNodeResponse(
        String id,
        String parentId,
        String code,
        String name,
        String path,
        int level,
        int sortOrder,
        String status,
        String imageUrl,
        List<CategoryTreeNodeResponse> children
) {
    public CategoryTreeNodeResponse(
            String id,
            String parentId,
            String code,
            String name,
            String path,
            int level,
            int sortOrder,
            String status,
            List<CategoryTreeNodeResponse> children
    ) {
        this(id, parentId, code, name, path, level, sortOrder, status, null, children);
    }
}
