UPDATE buyer_profiles
SET full_name = 'Abena Mensah',
    phone_number = '+233 24 567 8901',
    avatar_url = '/public/uploads/buyers/abena-mensah.jpg',
    bio = 'Loves discovering handmade finds across Ghana.',
    buyer_rating = 4.9,
    is_verified = TRUE,
    verification_label = 'Verified 🇬🇭',
    stats_order_count = 12,
    stats_wishlist_count = 8,
    stats_review_count = 5,
    active_order_count = 3,
    saved_address_count = 2,
    active_voucher_count = 1,
    updated_at = CURRENT_TIMESTAMP
WHERE buyer_id = '00000000-0000-0000-0000-000000000101';

INSERT INTO buyer_profiles (
    buyer_id,
    full_name,
    phone_number,
    avatar_url,
    bio,
    buyer_rating,
    is_verified,
    verification_label,
    stats_order_count,
    stats_wishlist_count,
    stats_review_count,
    active_order_count,
    saved_address_count,
    active_voucher_count,
    created_at,
    updated_at
)
SELECT
    '00000000-0000-0000-0000-000000000101',
    'Abena Mensah',
    '+233 24 567 8901',
    '/public/uploads/buyers/abena-mensah.jpg',
    'Loves discovering handmade finds across Ghana.',
    4.9,
    TRUE,
    'Verified 🇬🇭',
    12,
    8,
    5,
    3,
    2,
    1,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1
    FROM buyer_profiles
    WHERE buyer_id = '00000000-0000-0000-0000-000000000101'
);

UPDATE buyer_wallet_accounts
SET buyer_id = '00000000-0000-0000-0000-000000000101',
    provider_code = 'MTN_MOMO',
    provider_label = 'MTN MOMO WALLET',
    wallet_phone_number = '+233 24 567 8901',
    currency_code = 'GHS',
    balance_amount = 2480.00,
    is_balance_hidden = TRUE,
    is_default = TRUE,
    status = 'ACTIVE',
    updated_at = CURRENT_TIMESTAMP
WHERE id = 'wallet-abena-mtn-001';

INSERT INTO buyer_wallet_accounts (
    id,
    buyer_id,
    provider_code,
    provider_label,
    wallet_phone_number,
    currency_code,
    balance_amount,
    is_balance_hidden,
    is_default,
    status,
    created_at,
    updated_at
)
SELECT
    'wallet-abena-mtn-001',
    '00000000-0000-0000-0000-000000000101',
    'MTN_MOMO',
    'MTN MOMO WALLET',
    '+233 24 567 8901',
    'GHS',
    2480.00,
    TRUE,
    TRUE,
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1
    FROM buyer_wallet_accounts
    WHERE id = 'wallet-abena-mtn-001'
);

INSERT INTO merchants (id, merchant_code, merchant_type, display_name, status, created_at, updated_at)
SELECT 'buyer-me-demo-merchant-000000000001', 'ZM-ME-DEMO', 'SELF_OPERATED', 'ZokoMart Me Demo Store', 'APPROVED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1
    FROM merchants
    WHERE id = 'buyer-me-demo-merchant-000000000001'
);

UPDATE orders
SET order_number = 'SK-7821',
    buyer_id = '00000000-0000-0000-0000-000000000101',
    merchant_id = 'buyer-me-demo-merchant-000000000001',
    status = 'SHIPPED',
    currency_code = 'GHS',
    subtotal_amount = 425.00,
    shipping_amount = 0.00,
    discount_amount = 0.00,
    total_amount = 425.00,
    recipient_name_snapshot = 'Abena Mensah',
    phone_number_snapshot = '+233 24 567 8901',
    address_line1_snapshot = 'East Legon',
    address_line2_snapshot = 'Block B',
    city_snapshot = 'Accra',
    region_snapshot = 'Greater Accra',
    country_code_snapshot = 'GH',
    placed_at = TIMESTAMP '2026-04-18 09:15:00',
    cancelled_at = NULL,
    updated_at = TIMESTAMP '2026-04-18 09:15:00'
WHERE id = 'order-sk-7821-0000-0000-000000000001';

INSERT INTO orders (
    id,
    order_number,
    buyer_id,
    merchant_id,
    status,
    currency_code,
    subtotal_amount,
    shipping_amount,
    discount_amount,
    total_amount,
    recipient_name_snapshot,
    phone_number_snapshot,
    address_line1_snapshot,
    address_line2_snapshot,
    city_snapshot,
    region_snapshot,
    country_code_snapshot,
    placed_at,
    cancelled_at,
    created_at,
    updated_at
)
SELECT
    'order-sk-7821-0000-0000-000000000001',
    'SK-7821',
    '00000000-0000-0000-0000-000000000101',
    'buyer-me-demo-merchant-000000000001',
    'SHIPPED',
    'GHS',
    425.00,
    0.00,
    0.00,
    425.00,
    'Abena Mensah',
    '+233 24 567 8901',
    'East Legon',
    'Block B',
    'Accra',
    'Greater Accra',
    'GH',
    TIMESTAMP '2026-04-18 09:15:00',
    NULL,
    TIMESTAMP '2026-04-18 09:15:00',
    TIMESTAMP '2026-04-18 09:15:00'
WHERE NOT EXISTS (
    SELECT 1
    FROM orders
    WHERE id = 'order-sk-7821-0000-0000-000000000001'
);

UPDATE orders
SET order_number = 'SK-7804',
    buyer_id = '00000000-0000-0000-0000-000000000101',
    merchant_id = 'buyer-me-demo-merchant-000000000001',
    status = 'DELIVERED',
    currency_code = 'GHS',
    subtotal_amount = 145.00,
    shipping_amount = 0.00,
    discount_amount = 0.00,
    total_amount = 145.00,
    recipient_name_snapshot = 'Abena Mensah',
    phone_number_snapshot = '+233 24 567 8901',
    address_line1_snapshot = 'East Legon',
    address_line2_snapshot = 'Block B',
    city_snapshot = 'Accra',
    region_snapshot = 'Greater Accra',
    country_code_snapshot = 'GH',
    placed_at = TIMESTAMP '2026-04-12 14:20:00',
    cancelled_at = NULL,
    updated_at = TIMESTAMP '2026-04-12 14:20:00'
WHERE id = 'order-sk-7804-0000-0000-000000000002';

INSERT INTO orders (
    id,
    order_number,
    buyer_id,
    merchant_id,
    status,
    currency_code,
    subtotal_amount,
    shipping_amount,
    discount_amount,
    total_amount,
    recipient_name_snapshot,
    phone_number_snapshot,
    address_line1_snapshot,
    address_line2_snapshot,
    city_snapshot,
    region_snapshot,
    country_code_snapshot,
    placed_at,
    cancelled_at,
    created_at,
    updated_at
)
SELECT
    'order-sk-7804-0000-0000-000000000002',
    'SK-7804',
    '00000000-0000-0000-0000-000000000101',
    'buyer-me-demo-merchant-000000000001',
    'DELIVERED',
    'GHS',
    145.00,
    0.00,
    0.00,
    145.00,
    'Abena Mensah',
    '+233 24 567 8901',
    'East Legon',
    'Block B',
    'Accra',
    'Greater Accra',
    'GH',
    TIMESTAMP '2026-04-12 14:20:00',
    NULL,
    TIMESTAMP '2026-04-12 14:20:00',
    TIMESTAMP '2026-04-12 14:20:00'
WHERE NOT EXISTS (
    SELECT 1
    FROM orders
    WHERE id = 'order-sk-7804-0000-0000-000000000002'
);

UPDATE orders
SET order_number = 'SK-7799',
    buyer_id = '00000000-0000-0000-0000-000000000101',
    merchant_id = 'buyer-me-demo-merchant-000000000001',
    status = 'DELIVERED',
    currency_code = 'GHS',
    subtotal_amount = 1980.00,
    shipping_amount = 0.00,
    discount_amount = 0.00,
    total_amount = 1980.00,
    recipient_name_snapshot = 'Abena Mensah',
    phone_number_snapshot = '+233 24 567 8901',
    address_line1_snapshot = 'East Legon',
    address_line2_snapshot = 'Block B',
    city_snapshot = 'Accra',
    region_snapshot = 'Greater Accra',
    country_code_snapshot = 'GH',
    placed_at = TIMESTAMP '2026-04-10 11:45:00',
    cancelled_at = NULL,
    updated_at = TIMESTAMP '2026-04-10 11:45:00'
WHERE id = 'order-sk-7799-0000-0000-000000000003';

INSERT INTO orders (
    id,
    order_number,
    buyer_id,
    merchant_id,
    status,
    currency_code,
    subtotal_amount,
    shipping_amount,
    discount_amount,
    total_amount,
    recipient_name_snapshot,
    phone_number_snapshot,
    address_line1_snapshot,
    address_line2_snapshot,
    city_snapshot,
    region_snapshot,
    country_code_snapshot,
    placed_at,
    cancelled_at,
    created_at,
    updated_at
)
SELECT
    'order-sk-7799-0000-0000-000000000003',
    'SK-7799',
    '00000000-0000-0000-0000-000000000101',
    'buyer-me-demo-merchant-000000000001',
    'DELIVERED',
    'GHS',
    1980.00,
    0.00,
    0.00,
    1980.00,
    'Abena Mensah',
    '+233 24 567 8901',
    'East Legon',
    'Block B',
    'Accra',
    'Greater Accra',
    'GH',
    TIMESTAMP '2026-04-10 11:45:00',
    NULL,
    TIMESTAMP '2026-04-10 11:45:00',
    TIMESTAMP '2026-04-10 11:45:00'
WHERE NOT EXISTS (
    SELECT 1
    FROM orders
    WHERE id = 'order-sk-7799-0000-0000-000000000003'
);

UPDATE order_items
SET order_id = 'order-sk-7821-0000-0000-000000000001',
    product_id = '6db6eb8a-c00e-4c85-8bd8-6ae76fd4f8d5',
    sku_id = '77cfce71-f8f5-44d5-9adc-0a76d5f65d5a',
    merchant_id = 'buyer-me-demo-merchant-000000000001',
    product_name_snapshot = 'Zoko Phone X1',
    sku_name_snapshot = 'Black / 128GB',
    attributes_snapshot_json = '{"storage":"128GB","color":"black"}',
    unit_price_amount_snapshot = 225.00,
    currency_code = 'GHS',
    quantity = 1,
    line_total_amount = 225.00,
    created_at = TIMESTAMP '2026-04-18 09:15:00'
WHERE id = 'order-item-sk-7821-001';

INSERT INTO order_items (
    id,
    order_id,
    product_id,
    sku_id,
    merchant_id,
    product_name_snapshot,
    sku_name_snapshot,
    attributes_snapshot_json,
    unit_price_amount_snapshot,
    currency_code,
    quantity,
    line_total_amount,
    created_at
)
SELECT
    'order-item-sk-7821-001',
    'order-sk-7821-0000-0000-000000000001',
    '6db6eb8a-c00e-4c85-8bd8-6ae76fd4f8d5',
    '77cfce71-f8f5-44d5-9adc-0a76d5f65d5a',
    'buyer-me-demo-merchant-000000000001',
    'Zoko Phone X1',
    'Black / 128GB',
    '{"storage":"128GB","color":"black"}',
    225.00,
    'GHS',
    1,
    225.00,
    TIMESTAMP '2026-04-18 09:15:00'
WHERE NOT EXISTS (
    SELECT 1
    FROM order_items
    WHERE id = 'order-item-sk-7821-001'
);

UPDATE order_items
SET order_id = 'order-sk-7821-0000-0000-000000000001',
    product_id = '6db6eb8a-c00e-4c85-8bd8-6ae76fd4f8d5',
    sku_id = '42d76b3d-97be-44ab-aa59-0ab49f4c0c21',
    merchant_id = 'buyer-me-demo-merchant-000000000001',
    product_name_snapshot = 'Zoko Phone Case',
    sku_name_snapshot = 'Orange / Standard',
    attributes_snapshot_json = '{"color":"orange"}',
    unit_price_amount_snapshot = 200.00,
    currency_code = 'GHS',
    quantity = 1,
    line_total_amount = 200.00,
    created_at = TIMESTAMP '2026-04-18 09:15:00'
WHERE id = 'order-item-sk-7821-002';

INSERT INTO order_items (
    id,
    order_id,
    product_id,
    sku_id,
    merchant_id,
    product_name_snapshot,
    sku_name_snapshot,
    attributes_snapshot_json,
    unit_price_amount_snapshot,
    currency_code,
    quantity,
    line_total_amount,
    created_at
)
SELECT
    'order-item-sk-7821-002',
    'order-sk-7821-0000-0000-000000000001',
    '6db6eb8a-c00e-4c85-8bd8-6ae76fd4f8d5',
    '42d76b3d-97be-44ab-aa59-0ab49f4c0c21',
    'buyer-me-demo-merchant-000000000001',
    'Zoko Phone Case',
    'Orange / Standard',
    '{"color":"orange"}',
    200.00,
    'GHS',
    1,
    200.00,
    TIMESTAMP '2026-04-18 09:15:00'
WHERE NOT EXISTS (
    SELECT 1
    FROM order_items
    WHERE id = 'order-item-sk-7821-002'
);

UPDATE order_items
SET order_id = 'order-sk-7804-0000-0000-000000000002',
    product_id = '6db6eb8a-c00e-4c85-8bd8-6ae76fd4f8d5',
    sku_id = '77cfce71-f8f5-44d5-9adc-0a76d5f65d5a',
    merchant_id = 'buyer-me-demo-merchant-000000000001',
    product_name_snapshot = 'ZokoMart Essentials',
    sku_name_snapshot = 'Daily Pack',
    attributes_snapshot_json = '{}',
    unit_price_amount_snapshot = 145.00,
    currency_code = 'GHS',
    quantity = 1,
    line_total_amount = 145.00,
    created_at = TIMESTAMP '2026-04-12 14:20:00'
WHERE id = 'order-item-sk-7804-001';

INSERT INTO order_items (
    id,
    order_id,
    product_id,
    sku_id,
    merchant_id,
    product_name_snapshot,
    sku_name_snapshot,
    attributes_snapshot_json,
    unit_price_amount_snapshot,
    currency_code,
    quantity,
    line_total_amount,
    created_at
)
SELECT
    'order-item-sk-7804-001',
    'order-sk-7804-0000-0000-000000000002',
    '6db6eb8a-c00e-4c85-8bd8-6ae76fd4f8d5',
    '77cfce71-f8f5-44d5-9adc-0a76d5f65d5a',
    'buyer-me-demo-merchant-000000000001',
    'ZokoMart Essentials',
    'Daily Pack',
    '{}',
    145.00,
    'GHS',
    1,
    145.00,
    TIMESTAMP '2026-04-12 14:20:00'
WHERE NOT EXISTS (
    SELECT 1
    FROM order_items
    WHERE id = 'order-item-sk-7804-001'
);

UPDATE order_items
SET order_id = 'order-sk-7799-0000-0000-000000000003',
    product_id = '6db6eb8a-c00e-4c85-8bd8-6ae76fd4f8d5',
    sku_id = '42d76b3d-97be-44ab-aa59-0ab49f4c0c21',
    merchant_id = 'buyer-me-demo-merchant-000000000001',
    product_name_snapshot = 'Zoko Family Bundle',
    sku_name_snapshot = 'Bundle A',
    attributes_snapshot_json = '{}',
    unit_price_amount_snapshot = 660.00,
    currency_code = 'GHS',
    quantity = 3,
    line_total_amount = 1980.00,
    created_at = TIMESTAMP '2026-04-10 11:45:00'
WHERE id = 'order-item-sk-7799-001';

INSERT INTO order_items (
    id,
    order_id,
    product_id,
    sku_id,
    merchant_id,
    product_name_snapshot,
    sku_name_snapshot,
    attributes_snapshot_json,
    unit_price_amount_snapshot,
    currency_code,
    quantity,
    line_total_amount,
    created_at
)
SELECT
    'order-item-sk-7799-001',
    'order-sk-7799-0000-0000-000000000003',
    '6db6eb8a-c00e-4c85-8bd8-6ae76fd4f8d5',
    '42d76b3d-97be-44ab-aa59-0ab49f4c0c21',
    'buyer-me-demo-merchant-000000000001',
    'Zoko Family Bundle',
    'Bundle A',
    '{}',
    660.00,
    'GHS',
    3,
    1980.00,
    TIMESTAMP '2026-04-10 11:45:00'
WHERE NOT EXISTS (
    SELECT 1
    FROM order_items
    WHERE id = 'order-item-sk-7799-001'
);

UPDATE buyer_transactions
SET buyer_id = '00000000-0000-0000-0000-000000000101',
    wallet_account_id = 'wallet-abena-mtn-001',
    transaction_type = 'ORDER_PAYMENT',
    direction = 'DEBIT',
    title = 'ZokoMart Order #7821',
    reference_order_id = 'order-sk-7821-0000-0000-000000000001',
    amount = 425.00,
    currency_code = 'GHS',
    occurred_at = TIMESTAMP '2026-04-18 10:30:00',
    created_at = CURRENT_TIMESTAMP
WHERE id = 'txn-abena-order-7821';

INSERT INTO buyer_transactions (
    id,
    buyer_id,
    wallet_account_id,
    transaction_type,
    direction,
    title,
    reference_order_id,
    amount,
    currency_code,
    occurred_at,
    created_at
)
SELECT
    'txn-abena-order-7821',
    '00000000-0000-0000-0000-000000000101',
    'wallet-abena-mtn-001',
    'ORDER_PAYMENT',
    'DEBIT',
    'ZokoMart Order #7821',
    'order-sk-7821-0000-0000-000000000001',
    425.00,
    'GHS',
    TIMESTAMP '2026-04-18 10:30:00',
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1
    FROM buyer_transactions
    WHERE id = 'txn-abena-order-7821'
);

UPDATE buyer_transactions
SET buyer_id = '00000000-0000-0000-0000-000000000101',
    wallet_account_id = 'wallet-abena-mtn-001',
    transaction_type = 'MOMO_TOP_UP',
    direction = 'CREDIT',
    title = 'MoMo Top-up',
    reference_order_id = NULL,
    amount = 500.00,
    currency_code = 'GHS',
    occurred_at = TIMESTAMP '2026-04-16 08:45:00',
    created_at = CURRENT_TIMESTAMP
WHERE id = 'txn-abena-topup-0416';

INSERT INTO buyer_transactions (
    id,
    buyer_id,
    wallet_account_id,
    transaction_type,
    direction,
    title,
    reference_order_id,
    amount,
    currency_code,
    occurred_at,
    created_at
)
SELECT
    'txn-abena-topup-0416',
    '00000000-0000-0000-0000-000000000101',
    'wallet-abena-mtn-001',
    'MOMO_TOP_UP',
    'CREDIT',
    'MoMo Top-up',
    NULL,
    500.00,
    'GHS',
    TIMESTAMP '2026-04-16 08:45:00',
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1
    FROM buyer_transactions
    WHERE id = 'txn-abena-topup-0416'
);

UPDATE buyer_transactions
SET buyer_id = '00000000-0000-0000-0000-000000000101',
    wallet_account_id = 'wallet-abena-mtn-001',
    transaction_type = 'ORDER_PAYMENT',
    direction = 'DEBIT',
    title = 'ZokoMart Order #7804',
    reference_order_id = 'order-sk-7804-0000-0000-000000000002',
    amount = 145.00,
    currency_code = 'GHS',
    occurred_at = TIMESTAMP '2026-04-12 15:05:00',
    created_at = CURRENT_TIMESTAMP
WHERE id = 'txn-abena-order-7804';

INSERT INTO buyer_transactions (
    id,
    buyer_id,
    wallet_account_id,
    transaction_type,
    direction,
    title,
    reference_order_id,
    amount,
    currency_code,
    occurred_at,
    created_at
)
SELECT
    'txn-abena-order-7804',
    '00000000-0000-0000-0000-000000000101',
    'wallet-abena-mtn-001',
    'ORDER_PAYMENT',
    'DEBIT',
    'ZokoMart Order #7804',
    'order-sk-7804-0000-0000-000000000002',
    145.00,
    'GHS',
    TIMESTAMP '2026-04-12 15:05:00',
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1
    FROM buyer_transactions
    WHERE id = 'txn-abena-order-7804'
);

UPDATE buyer_saved_addresses
SET buyer_id = '00000000-0000-0000-0000-000000000101',
    label = 'Home',
    recipient_name = 'Abena Mensah',
    phone_number = '+233 24 567 8901',
    address_line1 = 'East Legon',
    address_line2 = 'Block B',
    city = 'Accra',
    region = 'Greater Accra',
    country_code = 'GH',
    is_default = TRUE,
    updated_at = CURRENT_TIMESTAMP
WHERE id = 'addr-abena-east-legon';

INSERT INTO buyer_saved_addresses (
    id,
    buyer_id,
    label,
    recipient_name,
    phone_number,
    address_line1,
    address_line2,
    city,
    region,
    country_code,
    is_default,
    created_at,
    updated_at
)
SELECT
    'addr-abena-east-legon',
    '00000000-0000-0000-0000-000000000101',
    'Home',
    'Abena Mensah',
    '+233 24 567 8901',
    'East Legon',
    'Block B',
    'Accra',
    'Greater Accra',
    'GH',
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1
    FROM buyer_saved_addresses
    WHERE id = 'addr-abena-east-legon'
);

UPDATE buyer_saved_addresses
SET buyer_id = '00000000-0000-0000-0000-000000000101',
    label = 'Office',
    recipient_name = 'Abena Mensah',
    phone_number = '+233 24 567 8901',
    address_line1 = 'Oxford Street',
    address_line2 = NULL,
    city = 'Accra',
    region = 'Greater Accra',
    country_code = 'GH',
    is_default = FALSE,
    updated_at = CURRENT_TIMESTAMP
WHERE id = 'addr-abena-osu';

INSERT INTO buyer_saved_addresses (
    id,
    buyer_id,
    label,
    recipient_name,
    phone_number,
    address_line1,
    address_line2,
    city,
    region,
    country_code,
    is_default,
    created_at,
    updated_at
)
SELECT
    'addr-abena-osu',
    '00000000-0000-0000-0000-000000000101',
    'Office',
    'Abena Mensah',
    '+233 24 567 8901',
    'Oxford Street',
    NULL,
    'Accra',
    'Greater Accra',
    'GH',
    FALSE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1
    FROM buyer_saved_addresses
    WHERE id = 'addr-abena-osu'
);
