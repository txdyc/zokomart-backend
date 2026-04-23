ALTER TABLE brands ADD COLUMN image_storage_key VARCHAR(255);
ALTER TABLE brands ADD COLUMN image_url VARCHAR(512);
ALTER TABLE brands ADD COLUMN image_content_type VARCHAR(128);
ALTER TABLE brands ADD COLUMN image_size_bytes BIGINT;
ALTER TABLE brands ADD COLUMN image_original_filename VARCHAR(255);

CREATE TABLE product_images (
    id VARCHAR(36) PRIMARY KEY,
    product_id VARCHAR(36) NOT NULL,
    storage_key VARCHAR(255) NOT NULL,
    image_url VARCHAR(512) NOT NULL,
    content_type VARCHAR(128) NOT NULL,
    size_bytes BIGINT NOT NULL,
    original_filename VARCHAR(255),
    sort_order INT NOT NULL,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_product_images_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE
);

CREATE INDEX idx_product_images_product_sort_order ON product_images (product_id, sort_order);
CREATE INDEX idx_product_images_product_primary ON product_images (product_id, is_primary);
