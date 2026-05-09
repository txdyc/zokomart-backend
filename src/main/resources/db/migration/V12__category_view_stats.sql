CREATE TABLE category_stats (
    id VARCHAR(36) PRIMARY KEY,
    category_id VARCHAR(36) NOT NULL,
    view_count BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_category_stats_category FOREIGN KEY (category_id) REFERENCES categories (id),
    CONSTRAINT uk_category_stats_category UNIQUE (category_id)
);

CREATE INDEX idx_category_stats_view_count ON category_stats (view_count DESC, updated_at DESC);
CREATE INDEX idx_category_stats_updated_at ON category_stats (updated_at DESC);
