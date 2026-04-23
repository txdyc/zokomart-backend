CREATE TABLE merchants (
    id VARCHAR(36) PRIMARY KEY,
    merchant_code VARCHAR(64) NOT NULL UNIQUE,
    merchant_type VARCHAR(32) NOT NULL,
    display_name VARCHAR(160) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE products (
    id VARCHAR(36) PRIMARY KEY,
    merchant_id VARCHAR(36) NOT NULL,
    product_code VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    description VARCHAR(500),
    status VARCHAR(32) NOT NULL,
    is_self_operated BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_products_merchant FOREIGN KEY (merchant_id) REFERENCES merchants (id)
);

CREATE INDEX idx_products_merchant_status ON products (merchant_id, status);

CREATE TABLE product_skus (
    id VARCHAR(36) PRIMARY KEY,
    product_id VARCHAR(36) NOT NULL,
    sku_code VARCHAR(80) NOT NULL UNIQUE,
    sku_name VARCHAR(160) NOT NULL,
    attributes_json VARCHAR(1000),
    unit_price_amount DECIMAL(12, 2) NOT NULL,
    currency_code VARCHAR(3) NOT NULL,
    available_quantity INT NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_product_skus_product FOREIGN KEY (product_id) REFERENCES products (id)
);

CREATE INDEX idx_product_skus_product_status ON product_skus (product_id, status);

CREATE TABLE carts (
    id VARCHAR(36) PRIMARY KEY,
    buyer_id VARCHAR(36) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_carts_buyer_status ON carts (buyer_id, status);

CREATE TABLE cart_items (
    id VARCHAR(36) PRIMARY KEY,
    cart_id VARCHAR(36) NOT NULL,
    merchant_id VARCHAR(36) NOT NULL,
    product_id VARCHAR(36) NOT NULL,
    sku_id VARCHAR(36) NOT NULL,
    product_name_snapshot VARCHAR(200) NOT NULL,
    sku_name_snapshot VARCHAR(160) NOT NULL,
    reference_price_amount DECIMAL(12, 2) NOT NULL,
    currency_code VARCHAR(3) NOT NULL,
    quantity INT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_cart_items_cart FOREIGN KEY (cart_id) REFERENCES carts (id),
    CONSTRAINT fk_cart_items_merchant FOREIGN KEY (merchant_id) REFERENCES merchants (id),
    CONSTRAINT fk_cart_items_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT fk_cart_items_sku FOREIGN KEY (sku_id) REFERENCES product_skus (id)
);

CREATE INDEX idx_cart_items_cart_id ON cart_items (cart_id);

CREATE TABLE orders (
    id VARCHAR(36) PRIMARY KEY,
    order_number VARCHAR(40) NOT NULL UNIQUE,
    buyer_id VARCHAR(36) NOT NULL,
    merchant_id VARCHAR(36) NOT NULL,
    status VARCHAR(32) NOT NULL,
    currency_code VARCHAR(3) NOT NULL,
    subtotal_amount DECIMAL(12, 2) NOT NULL,
    shipping_amount DECIMAL(12, 2) NOT NULL,
    discount_amount DECIMAL(12, 2) NOT NULL,
    total_amount DECIMAL(12, 2) NOT NULL,
    recipient_name_snapshot VARCHAR(160) NOT NULL,
    phone_number_snapshot VARCHAR(40) NOT NULL,
    address_line1_snapshot VARCHAR(200) NOT NULL,
    address_line2_snapshot VARCHAR(200),
    city_snapshot VARCHAR(120) NOT NULL,
    region_snapshot VARCHAR(120),
    country_code_snapshot VARCHAR(3) NOT NULL,
    placed_at TIMESTAMP NOT NULL,
    cancelled_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_orders_merchant FOREIGN KEY (merchant_id) REFERENCES merchants (id)
);

CREATE INDEX idx_orders_buyer_created_at ON orders (buyer_id, created_at DESC);
CREATE INDEX idx_orders_merchant_created_at ON orders (merchant_id, created_at DESC);
CREATE INDEX idx_orders_status_created_at ON orders (status, created_at DESC);

CREATE TABLE order_items (
    id VARCHAR(36) PRIMARY KEY,
    order_id VARCHAR(36) NOT NULL,
    product_id VARCHAR(36) NOT NULL,
    sku_id VARCHAR(36) NOT NULL,
    merchant_id VARCHAR(36) NOT NULL,
    product_name_snapshot VARCHAR(200) NOT NULL,
    sku_name_snapshot VARCHAR(160) NOT NULL,
    attributes_snapshot_json VARCHAR(1000),
    unit_price_amount_snapshot DECIMAL(12, 2) NOT NULL,
    currency_code VARCHAR(3) NOT NULL,
    quantity INT NOT NULL,
    line_total_amount DECIMAL(12, 2) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT fk_order_items_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT fk_order_items_sku FOREIGN KEY (sku_id) REFERENCES product_skus (id),
    CONSTRAINT fk_order_items_merchant FOREIGN KEY (merchant_id) REFERENCES merchants (id)
);

CREATE INDEX idx_order_items_order_id ON order_items (order_id);

CREATE TABLE payment_intents (
    id VARCHAR(36) PRIMARY KEY,
    payment_intent_number VARCHAR(40) NOT NULL UNIQUE,
    order_id VARCHAR(36) NOT NULL UNIQUE,
    buyer_id VARCHAR(36) NOT NULL,
    status VARCHAR(32) NOT NULL,
    amount DECIMAL(12, 2) NOT NULL,
    currency_code VARCHAR(3) NOT NULL,
    provider_code VARCHAR(40),
    expires_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_payment_intents_order FOREIGN KEY (order_id) REFERENCES orders (id)
);

CREATE INDEX idx_payment_intents_status_created_at ON payment_intents (status, created_at DESC);

CREATE TABLE order_status_history (
    id VARCHAR(36) PRIMARY KEY,
    order_id VARCHAR(36) NOT NULL,
    from_status VARCHAR(32),
    to_status VARCHAR(32) NOT NULL,
    changed_by_actor_type VARCHAR(32) NOT NULL,
    changed_by_actor_id VARCHAR(36),
    reason_code VARCHAR(64),
    notes VARCHAR(500),
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_order_status_history_order FOREIGN KEY (order_id) REFERENCES orders (id)
);

CREATE INDEX idx_order_status_history_order_id_created_at ON order_status_history (order_id, created_at DESC);

CREATE TABLE fulfillment_records (
    id VARCHAR(36) PRIMARY KEY,
    order_id VARCHAR(36) NOT NULL UNIQUE,
    merchant_id VARCHAR(36) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_fulfillment_records_order FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT fk_fulfillment_records_merchant FOREIGN KEY (merchant_id) REFERENCES merchants (id)
);

CREATE TABLE fulfillment_events (
    id VARCHAR(36) PRIMARY KEY,
    fulfillment_record_id VARCHAR(36) NOT NULL,
    from_status VARCHAR(32),
    to_status VARCHAR(32) NOT NULL,
    changed_by_actor_id VARCHAR(36) NOT NULL,
    notes VARCHAR(500),
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_fulfillment_events_record FOREIGN KEY (fulfillment_record_id) REFERENCES fulfillment_records (id)
);

CREATE INDEX idx_fulfillment_events_record_created_at ON fulfillment_events (fulfillment_record_id, created_at DESC);
