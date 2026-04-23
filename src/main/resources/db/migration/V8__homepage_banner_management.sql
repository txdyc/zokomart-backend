CREATE TABLE homepage_banners (
    id VARCHAR(64) PRIMARY KEY,
    title VARCHAR(128) NOT NULL,
    image_storage_key VARCHAR(255) NOT NULL,
    image_url VARCHAR(512) NOT NULL,
    image_content_type VARCHAR(128) NOT NULL,
    image_size_bytes BIGINT NOT NULL,
    image_original_filename VARCHAR(255) NOT NULL,
    target_type VARCHAR(32) NOT NULL,
    target_product_id VARCHAR(64),
    target_activity_key VARCHAR(128),
    sort_order INT NOT NULL DEFAULT 0,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_homepage_banners_enabled_sort_order
    ON homepage_banners (enabled, sort_order, updated_at);

ALTER TABLE homepage_banners
    ADD CONSTRAINT chk_homepage_banner_target_type
    CHECK (target_type IN ('PRODUCT_DETAIL', 'ACTIVITY_PAGE'));

ALTER TABLE homepage_banners
    ADD CONSTRAINT chk_homepage_banner_target_fields
    CHECK (
        (target_type = 'PRODUCT_DETAIL' AND target_product_id IS NOT NULL AND target_activity_key IS NULL)
            OR
        (target_type = 'ACTIVITY_PAGE' AND target_product_id IS NULL AND target_activity_key IS NOT NULL)
    );

