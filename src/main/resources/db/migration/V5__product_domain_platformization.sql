CREATE TABLE brands (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(160) NOT NULL,
    code VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    source_type VARCHAR(32) NOT NULL,
    created_by_merchant_id VARCHAR(36),
    approved_by_admin_id VARCHAR(36),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_brands_code UNIQUE (code),
    CONSTRAINT fk_brands_created_by_merchant FOREIGN KEY (created_by_merchant_id) REFERENCES merchants (id),
    CONSTRAINT fk_brands_approved_by_admin FOREIGN KEY (approved_by_admin_id) REFERENCES admin_users (id)
);

CREATE INDEX idx_brands_status_source_type ON brands (status, source_type);

ALTER TABLE products ADD COLUMN IF NOT EXISTS brand_id VARCHAR(36);
ALTER TABLE products ADD COLUMN IF NOT EXISTS description_html VARCHAR(4000);
ALTER TABLE products ADD COLUMN IF NOT EXISTS attributes_json VARCHAR(2000) DEFAULT '{}';
ALTER TABLE products ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;

UPDATE products
SET description_html = description
WHERE description_html IS NULL;

UPDATE products
SET attributes_json = '{}'
WHERE attributes_json IS NULL;

ALTER TABLE products
    ADD CONSTRAINT fk_products_brand
        FOREIGN KEY (brand_id) REFERENCES brands (id);

CREATE INDEX idx_products_category_id ON products (category_id);
CREATE INDEX idx_products_status_deleted_at ON products (status, deleted_at);
CREATE INDEX idx_products_attributes_gin ON products (attributes_json);

ALTER TABLE product_skus ADD COLUMN IF NOT EXISTS spu_id VARCHAR(36);
ALTER TABLE product_skus ADD COLUMN IF NOT EXISTS specs_json VARCHAR(2000);
ALTER TABLE product_skus ADD COLUMN IF NOT EXISTS price DECIMAL(12, 2);
ALTER TABLE product_skus ADD COLUMN IF NOT EXISTS original_price DECIMAL(12, 2);
ALTER TABLE product_skus ADD COLUMN IF NOT EXISTS cost_price DECIMAL(12, 2);
ALTER TABLE product_skus ADD COLUMN IF NOT EXISTS stock INT DEFAULT 0;
ALTER TABLE product_skus ADD COLUMN IF NOT EXISTS locked_stock INT DEFAULT 0;
ALTER TABLE product_skus ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;

UPDATE product_skus
SET spu_id = product_id
WHERE spu_id IS NULL;

UPDATE product_skus
SET specs_json = attributes_json
WHERE specs_json IS NULL;

UPDATE product_skus
SET price = unit_price_amount
WHERE price IS NULL;

UPDATE product_skus
SET original_price = unit_price_amount
WHERE original_price IS NULL;

UPDATE product_skus
SET cost_price = unit_price_amount
WHERE cost_price IS NULL;

UPDATE product_skus
SET stock = available_quantity
WHERE stock IS NULL;

UPDATE product_skus
SET locked_stock = 0
WHERE locked_stock IS NULL;

ALTER TABLE product_skus
    ADD CONSTRAINT fk_product_skus_spu
        FOREIGN KEY (spu_id) REFERENCES products (id);

CREATE INDEX idx_product_skus_price ON product_skus (price);
CREATE INDEX idx_product_skus_spu_status_deleted_at ON product_skus (spu_id, status, deleted_at);

ALTER TABLE categories ADD COLUMN IF NOT EXISTS parent_id VARCHAR(36);
ALTER TABLE categories ADD COLUMN IF NOT EXISTS code VARCHAR(64);
ALTER TABLE categories ADD COLUMN IF NOT EXISTS path VARCHAR(500);
ALTER TABLE categories ADD COLUMN IF NOT EXISTS level INT DEFAULT 1;
ALTER TABLE categories ADD COLUMN IF NOT EXISTS sort_order INT DEFAULT 0;
ALTER TABLE categories ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;

UPDATE categories
SET code = CASE
    WHEN id = 'f6f2c39a-1438-4e90-bcb2-bcb4db719001' THEN 'mobile-phones'
    ELSE LOWER(REPLACE(name, ' ', '-'))
END
WHERE code IS NULL;

UPDATE categories
SET path = '/' || code
WHERE path IS NULL;

UPDATE categories
SET level = 1
WHERE level IS NULL;

ALTER TABLE categories
    ADD CONSTRAINT fk_categories_parent
        FOREIGN KEY (parent_id) REFERENCES categories (id);

CREATE INDEX idx_categories_parent_id ON categories (parent_id);
CREATE INDEX idx_categories_path ON categories (path);

CREATE TABLE attributes (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(160) NOT NULL,
    code VARCHAR(64) NOT NULL,
    type VARCHAR(32) NOT NULL,
    category_id VARCHAR(36) NOT NULL,
    filterable BOOLEAN NOT NULL,
    searchable BOOLEAN NOT NULL,
    required BOOLEAN NOT NULL,
    is_custom_allowed BOOLEAN NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_attributes_category FOREIGN KEY (category_id) REFERENCES categories (id),
    CONSTRAINT uk_attributes_category_code UNIQUE (category_id, code)
);

CREATE INDEX idx_attributes_category_status ON attributes (category_id, status);

CREATE TABLE product_attribute_values (
    id VARCHAR(36) PRIMARY KEY,
    spu_id VARCHAR(36) NOT NULL,
    attribute_id VARCHAR(36),
    value_text VARCHAR(500),
    value_number DECIMAL(18, 4),
    value_boolean BOOLEAN,
    value_json VARCHAR(2000),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_product_attribute_values_spu FOREIGN KEY (spu_id) REFERENCES products (id),
    CONSTRAINT fk_product_attribute_values_attribute FOREIGN KEY (attribute_id) REFERENCES attributes (id)
);

CREATE INDEX idx_product_attribute_values_attribute_text ON product_attribute_values (attribute_id, value_text);
CREATE INDEX idx_product_attribute_values_attribute_number ON product_attribute_values (attribute_id, value_number);

CREATE TABLE merchant_custom_attributes (
    id VARCHAR(36) PRIMARY KEY,
    merchant_id VARCHAR(36) NOT NULL,
    category_id VARCHAR(36) NOT NULL,
    name VARCHAR(160) NOT NULL,
    type VARCHAR(32) NOT NULL,
    filterable BOOLEAN NOT NULL,
    searchable BOOLEAN NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_merchant_custom_attributes_merchant FOREIGN KEY (merchant_id) REFERENCES merchants (id),
    CONSTRAINT fk_merchant_custom_attributes_category FOREIGN KEY (category_id) REFERENCES categories (id)
);

CREATE INDEX idx_merchant_custom_attributes_merchant_category ON merchant_custom_attributes (merchant_id, category_id, status);

CREATE TABLE inventory_lock_records (
    id VARCHAR(36) PRIMARY KEY,
    sku_id VARCHAR(36) NOT NULL,
    order_id VARCHAR(36) NOT NULL,
    quantity INT NOT NULL,
    status VARCHAR(32) NOT NULL,
    expires_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_inventory_lock_records_sku FOREIGN KEY (sku_id) REFERENCES product_skus (id),
    CONSTRAINT fk_inventory_lock_records_order FOREIGN KEY (order_id) REFERENCES orders (id)
);

CREATE INDEX idx_inventory_lock_records_order_status ON inventory_lock_records (order_id, status);
CREATE INDEX idx_inventory_lock_records_sku_status ON inventory_lock_records (sku_id, status);
CREATE INDEX idx_inventory_lock_records_expires_at ON inventory_lock_records (expires_at);
