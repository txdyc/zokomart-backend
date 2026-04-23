CREATE TABLE buyer_profiles (
    buyer_id VARCHAR(36) PRIMARY KEY,
    full_name VARCHAR(160) NOT NULL,
    phone_number VARCHAR(40) NOT NULL,
    avatar_url VARCHAR(500),
    buyer_rating DECIMAL(3, 1) NOT NULL,
    is_verified BOOLEAN NOT NULL,
    verification_label VARCHAR(80) NOT NULL,
    stats_order_count INT NOT NULL,
    stats_wishlist_count INT NOT NULL,
    stats_review_count INT NOT NULL,
    active_order_count INT NOT NULL,
    saved_address_count INT NOT NULL,
    active_voucher_count INT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE buyer_wallet_accounts (
    id VARCHAR(36) PRIMARY KEY,
    buyer_id VARCHAR(36) NOT NULL,
    provider_code VARCHAR(32) NOT NULL,
    provider_label VARCHAR(80) NOT NULL,
    wallet_phone_number VARCHAR(40) NOT NULL,
    currency_code VARCHAR(3) NOT NULL,
    balance_amount DECIMAL(12, 2) NOT NULL,
    is_balance_hidden BOOLEAN NOT NULL,
    is_default BOOLEAN NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_buyer_wallet_accounts_profile FOREIGN KEY (buyer_id) REFERENCES buyer_profiles (buyer_id)
);

CREATE INDEX idx_buyer_wallet_accounts_buyer_default ON buyer_wallet_accounts (buyer_id, is_default, status);

CREATE TABLE buyer_transactions (
    id VARCHAR(36) PRIMARY KEY,
    buyer_id VARCHAR(36) NOT NULL,
    wallet_account_id VARCHAR(36),
    transaction_type VARCHAR(32) NOT NULL,
    direction VARCHAR(16) NOT NULL,
    title VARCHAR(160) NOT NULL,
    reference_order_id VARCHAR(36),
    amount DECIMAL(12, 2) NOT NULL,
    currency_code VARCHAR(3) NOT NULL,
    occurred_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_buyer_transactions_profile FOREIGN KEY (buyer_id) REFERENCES buyer_profiles (buyer_id),
    CONSTRAINT fk_buyer_transactions_wallet FOREIGN KEY (wallet_account_id) REFERENCES buyer_wallet_accounts (id),
    CONSTRAINT fk_buyer_transactions_order FOREIGN KEY (reference_order_id) REFERENCES orders (id)
);

CREATE INDEX idx_buyer_transactions_buyer_occurred_at ON buyer_transactions (buyer_id, occurred_at DESC);

CREATE TABLE buyer_saved_addresses (
    id VARCHAR(36) PRIMARY KEY,
    buyer_id VARCHAR(36) NOT NULL,
    label VARCHAR(80) NOT NULL,
    recipient_name VARCHAR(160) NOT NULL,
    phone_number VARCHAR(40) NOT NULL,
    address_line1 VARCHAR(200) NOT NULL,
    address_line2 VARCHAR(200),
    city VARCHAR(120) NOT NULL,
    region VARCHAR(120),
    country_code VARCHAR(3) NOT NULL,
    is_default BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_buyer_saved_addresses_profile FOREIGN KEY (buyer_id) REFERENCES buyer_profiles (buyer_id)
);

CREATE INDEX idx_buyer_saved_addresses_buyer_created_at ON buyer_saved_addresses (buyer_id, created_at DESC);

