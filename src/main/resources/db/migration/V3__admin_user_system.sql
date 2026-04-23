CREATE TABLE admin_users (
    id VARCHAR(36) PRIMARY KEY,
    username VARCHAR(64) NOT NULL,
    display_name VARCHAR(160) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    user_type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    last_login_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_admin_users_username UNIQUE (username)
);

CREATE INDEX idx_admin_users_type_status ON admin_users (user_type, status);

CREATE TABLE admin_user_merchants (
    id VARCHAR(36) PRIMARY KEY,
    admin_user_id VARCHAR(36) NOT NULL,
    merchant_id VARCHAR(36) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_admin_user_merchants_admin_user FOREIGN KEY (admin_user_id) REFERENCES admin_users (id),
    CONSTRAINT fk_admin_user_merchants_merchant FOREIGN KEY (merchant_id) REFERENCES merchants (id),
    CONSTRAINT uk_admin_user_merchants_binding UNIQUE (admin_user_id, merchant_id)
);

CREATE INDEX idx_admin_user_merchants_admin_user ON admin_user_merchants (admin_user_id);
CREATE INDEX idx_admin_user_merchants_merchant ON admin_user_merchants (merchant_id);

INSERT INTO admin_users (
    id,
    username,
    display_name,
    password_hash,
    user_type,
    status,
    last_login_at,
    created_at,
    updated_at
)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'platform.root',
    'Platform Root',
    'pbkdf2-sha256$310000$QWRtaW4tU2VlZC0yMDI2IQ==$cEFKomdOKsSly0xDaWVmRtTCPIx7HaHKR+8AYWwrwNo=',
    'PLATFORM_ADMIN',
    'ACTIVE',
    NULL,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
