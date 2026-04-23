ALTER TABLE categories
    ADD COLUMN image_storage_key VARCHAR(255);

ALTER TABLE categories
    ADD COLUMN image_url VARCHAR(512);

ALTER TABLE categories
    ADD COLUMN image_content_type VARCHAR(128);

ALTER TABLE categories
    ADD COLUMN image_size_bytes BIGINT;

ALTER TABLE categories
    ADD COLUMN image_original_filename VARCHAR(255);
