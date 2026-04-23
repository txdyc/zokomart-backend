package com.zokomart.backend.common.storage;

public record StoredObjectResult(
        String storageKey,
        String publicUrl,
        String contentType,
        long sizeBytes,
        String originalFilename
) {
}

