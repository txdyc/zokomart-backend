package com.zokomart.backend.common.storage;

public interface StorageService {

    StoredObjectResult store(StorageObjectType type, String originalFilename, String contentType, byte[] bytes);

    void delete(String storageKey);
}

