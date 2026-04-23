package com.zokomart.backend.common.storage;

public enum StorageObjectType {
    CATEGORY_IMAGE("categories"),
    BRAND_IMAGE("brands"),
    PRODUCT_IMAGE("products"),
    HOMEPAGE_BANNER("homepage-banners");

    private final String directory;

    StorageObjectType(String directory) {
        this.directory = directory;
    }

    public String directory() {
        return directory;
    }
}
