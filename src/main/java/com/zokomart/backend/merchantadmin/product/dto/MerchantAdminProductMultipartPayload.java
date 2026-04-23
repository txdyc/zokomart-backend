package com.zokomart.backend.merchantadmin.product.dto;

import java.util.List;

public record MerchantAdminProductMultipartPayload(
        List<String> retainImageIds,
        List<String> newImageClientIds,
        List<String> imageOrder
) {
    public List<String> safeRetainImageIds() {
        return retainImageIds == null ? List.of() : retainImageIds;
    }

    public List<String> safeNewImageClientIds() {
        return newImageClientIds == null ? List.of() : newImageClientIds;
    }

    public List<String> safeImageOrder() {
        return imageOrder == null ? List.of() : imageOrder;
    }
}
