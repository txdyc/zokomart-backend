CREATE TABLE buyer_auth_accounts (
    buyer_id VARCHAR(36) PRIMARY KEY,
    phone_number VARCHAR(40) NOT NULL,
    phone_number_normalized VARCHAR(40) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL,
    last_login_at TIMESTAMP,
    password_updated_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_buyer_auth_accounts_profile
        FOREIGN KEY (buyer_id) REFERENCES buyer_profiles (buyer_id)
);

CREATE UNIQUE INDEX uq_buyer_auth_accounts_phone_number_normalized
    ON buyer_auth_accounts (phone_number_normalized);
