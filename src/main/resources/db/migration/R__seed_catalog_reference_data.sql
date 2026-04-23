UPDATE categories
SET category_code = 'CAT-PHONES',
    code = 'mobile-phones',
    name = 'Mobile Phones',
    description = 'Phones and handheld devices',
    path = '/mobile-phones',
    level = 1,
    sort_order = 10,
    status = 'ACTIVE',
    updated_at = CURRENT_TIMESTAMP
WHERE id = 'f6f2c39a-1438-4e90-bcb2-bcb4db719001';

INSERT INTO categories (id, category_code, code, name, description, path, level, sort_order, status, created_at, updated_at)
SELECT 'f6f2c39a-1438-4e90-bcb2-bcb4db719001', 'CAT-PHONES', 'mobile-phones', 'Mobile Phones', 'Phones and handheld devices', '/mobile-phones', 1, 10, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1
    FROM categories
    WHERE id = 'f6f2c39a-1438-4e90-bcb2-bcb4db719001'
);

UPDATE brands
SET name = 'Tecno',
    code = 'tecno',
    status = 'APPROVED',
    source_type = 'PLATFORM',
    created_by_merchant_id = NULL,
    approved_by_admin_id = '00000000-0000-0000-0000-000000000001',
    updated_at = CURRENT_TIMESTAMP
WHERE id = 'brand-tecno-001';

INSERT INTO brands (id, name, code, status, source_type, created_by_merchant_id, approved_by_admin_id, created_at, updated_at)
SELECT 'brand-tecno-001', 'Tecno', 'tecno', 'APPROVED', 'PLATFORM', NULL, '00000000-0000-0000-0000-000000000001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1
    FROM brands
    WHERE id = 'brand-tecno-001'
);

UPDATE attributes
SET name = 'Screen Size',
    code = 'screen_size',
    type = 'TEXT',
    category_id = 'f6f2c39a-1438-4e90-bcb2-bcb4db719001',
    filterable = TRUE,
    searchable = TRUE,
    required = TRUE,
    is_custom_allowed = FALSE,
    status = 'ACTIVE',
    updated_at = CURRENT_TIMESTAMP
WHERE id = 'attr-screen-size-001';

INSERT INTO attributes (
    id,
    name,
    code,
    type,
    category_id,
    filterable,
    searchable,
    required,
    is_custom_allowed,
    status,
    created_at,
    updated_at
)
SELECT
    'attr-screen-size-001',
    'Screen Size',
    'screen_size',
    'TEXT',
    'f6f2c39a-1438-4e90-bcb2-bcb4db719001',
    TRUE,
    TRUE,
    TRUE,
    FALSE,
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1
    FROM attributes
    WHERE id = 'attr-screen-size-001'
);

UPDATE attributes
SET name = 'Network',
    code = 'network',
    type = 'TEXT',
    category_id = 'f6f2c39a-1438-4e90-bcb2-bcb4db719001',
    filterable = TRUE,
    searchable = TRUE,
    required = FALSE,
    is_custom_allowed = FALSE,
    status = 'ACTIVE',
    updated_at = CURRENT_TIMESTAMP
WHERE id = 'attr-network-001';

INSERT INTO attributes (
    id,
    name,
    code,
    type,
    category_id,
    filterable,
    searchable,
    required,
    is_custom_allowed,
    status,
    created_at,
    updated_at
)
SELECT
    'attr-network-001',
    'Network',
    'network',
    'TEXT',
    'f6f2c39a-1438-4e90-bcb2-bcb4db719001',
    TRUE,
    TRUE,
    FALSE,
    FALSE,
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1
    FROM attributes
    WHERE id = 'attr-network-001'
);

UPDATE merchants
SET merchant_code = 'ZM-SELF-001',
    merchant_type = 'SELF_OPERATED',
    display_name = 'ZokoMart Official Store',
    status = 'APPROVED',
    updated_at = CURRENT_TIMESTAMP
WHERE id = 'c5ce3f6d-3ca0-4b44-84a0-d2bc3f520fa3';

INSERT INTO merchants (id, merchant_code, merchant_type, display_name, status, created_at, updated_at)
SELECT 'c5ce3f6d-3ca0-4b44-84a0-d2bc3f520fa3', 'ZM-SELF-001', 'SELF_OPERATED', 'ZokoMart Official Store', 'APPROVED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1
    FROM merchants
    WHERE id = 'c5ce3f6d-3ca0-4b44-84a0-d2bc3f520fa3'
);

UPDATE products
SET merchant_id = 'c5ce3f6d-3ca0-4b44-84a0-d2bc3f520fa3',
    product_code = 'PRD-X1',
    name = 'Zoko Phone X1',
    description = '一款面向加纳市场的高性价比智能手机',
    description_html = '<p>一款面向加纳市场的高性价比智能手机</p>',
    status = 'APPROVED',
    is_self_operated = TRUE,
    category_id = 'f6f2c39a-1438-4e90-bcb2-bcb4db719001',
    brand_id = 'brand-tecno-001',
    attributes_json = '{"screen_size":"6.7 inch","network":"4G"}',
    updated_at = CURRENT_TIMESTAMP
WHERE id = '6db6eb8a-c00e-4c85-8bd8-6ae76fd4f8d5';

INSERT INTO products (
    id,
    merchant_id,
    product_code,
    name,
    description,
    description_html,
    status,
    is_self_operated,
    category_id,
    brand_id,
    attributes_json,
    created_at,
    updated_at
)
SELECT
    '6db6eb8a-c00e-4c85-8bd8-6ae76fd4f8d5',
    'c5ce3f6d-3ca0-4b44-84a0-d2bc3f520fa3',
    'PRD-X1',
    'Zoko Phone X1',
    '一款面向加纳市场的高性价比智能手机',
    '<p>一款面向加纳市场的高性价比智能手机</p>',
    'APPROVED',
    TRUE,
    'f6f2c39a-1438-4e90-bcb2-bcb4db719001',
    'brand-tecno-001',
    '{"screen_size":"6.7 inch","network":"4G"}',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1
    FROM products
    WHERE id = '6db6eb8a-c00e-4c85-8bd8-6ae76fd4f8d5'
);

UPDATE product_skus
SET product_id = '6db6eb8a-c00e-4c85-8bd8-6ae76fd4f8d5',
    spu_id = '6db6eb8a-c00e-4c85-8bd8-6ae76fd4f8d5',
    sku_code = 'X1-BLACK-128',
    sku_name = 'Black / 128GB',
    attributes_json = '{"storage":"128GB","color":"black"}',
    specs_json = '{"storage":"128GB","color":"black"}',
    unit_price_amount = 1250.00,
    price = 1250.00,
    original_price = 1350.00,
    cost_price = 900.00,
    currency_code = 'GHS',
    available_quantity = 15,
    stock = 15,
    locked_stock = 0,
    status = 'ACTIVE',
    updated_at = CURRENT_TIMESTAMP
WHERE id = '77cfce71-f8f5-44d5-9adc-0a76d5f65d5a';

INSERT INTO product_skus (
    id,
    product_id,
    spu_id,
    sku_code,
    sku_name,
    attributes_json,
    specs_json,
    unit_price_amount,
    price,
    original_price,
    cost_price,
    currency_code,
    available_quantity,
    stock,
    locked_stock,
    status,
    created_at,
    updated_at
)
SELECT
    '77cfce71-f8f5-44d5-9adc-0a76d5f65d5a',
    '6db6eb8a-c00e-4c85-8bd8-6ae76fd4f8d5',
    '6db6eb8a-c00e-4c85-8bd8-6ae76fd4f8d5',
    'X1-BLACK-128',
    'Black / 128GB',
    '{"storage":"128GB","color":"black"}',
    '{"storage":"128GB","color":"black"}',
    1250.00,
    1250.00,
    1350.00,
    900.00,
    'GHS',
    15,
    15,
    0,
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1
    FROM product_skus
    WHERE id = '77cfce71-f8f5-44d5-9adc-0a76d5f65d5a'
);

UPDATE product_skus
SET product_id = '6db6eb8a-c00e-4c85-8bd8-6ae76fd4f8d5',
    spu_id = '6db6eb8a-c00e-4c85-8bd8-6ae76fd4f8d5',
    sku_code = 'X1-BLUE-256',
    sku_name = 'Blue / 256GB',
    attributes_json = '{"storage":"256GB","color":"blue"}',
    specs_json = '{"storage":"256GB","color":"blue"}',
    unit_price_amount = 1450.00,
    price = 1450.00,
    original_price = 1590.00,
    cost_price = 980.00,
    currency_code = 'GHS',
    available_quantity = 9,
    stock = 9,
    locked_stock = 0,
    status = 'ACTIVE',
    updated_at = CURRENT_TIMESTAMP
WHERE id = '42d76b3d-97be-44ab-aa59-0ab49f4c0c21';

INSERT INTO product_skus (
    id,
    product_id,
    spu_id,
    sku_code,
    sku_name,
    attributes_json,
    specs_json,
    unit_price_amount,
    price,
    original_price,
    cost_price,
    currency_code,
    available_quantity,
    stock,
    locked_stock,
    status,
    created_at,
    updated_at
)
SELECT
    '42d76b3d-97be-44ab-aa59-0ab49f4c0c21',
    '6db6eb8a-c00e-4c85-8bd8-6ae76fd4f8d5',
    '6db6eb8a-c00e-4c85-8bd8-6ae76fd4f8d5',
    'X1-BLUE-256',
    'Blue / 256GB',
    '{"storage":"256GB","color":"blue"}',
    '{"storage":"256GB","color":"blue"}',
    1450.00,
    1450.00,
    1590.00,
    980.00,
    'GHS',
    9,
    9,
    0,
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1
    FROM product_skus
    WHERE id = '42d76b3d-97be-44ab-aa59-0ab49f4c0c21'
);

UPDATE product_skus
SET product_id = '6db6eb8a-c00e-4c85-8bd8-6ae76fd4f8d5',
    spu_id = '6db6eb8a-c00e-4c85-8bd8-6ae76fd4f8d5',
    sku_code = 'X1-ORANGE-512',
    sku_name = 'Orange / 512GB',
    attributes_json = '{"storage":"512GB","color":"orange"}',
    specs_json = '{"storage":"512GB","color":"orange"}',
    unit_price_amount = 1650.00,
    price = 1650.00,
    original_price = 1820.00,
    cost_price = 1100.00,
    currency_code = 'GHS',
    available_quantity = 0,
    stock = 0,
    locked_stock = 0,
    status = 'ACTIVE',
    updated_at = CURRENT_TIMESTAMP
WHERE id = '5a26c4c7-3b8f-4870-bf88-a4f44a4468c8';

INSERT INTO product_skus (
    id,
    product_id,
    spu_id,
    sku_code,
    sku_name,
    attributes_json,
    specs_json,
    unit_price_amount,
    price,
    original_price,
    cost_price,
    currency_code,
    available_quantity,
    stock,
    locked_stock,
    status,
    created_at,
    updated_at
)
SELECT
    '5a26c4c7-3b8f-4870-bf88-a4f44a4468c8',
    '6db6eb8a-c00e-4c85-8bd8-6ae76fd4f8d5',
    '6db6eb8a-c00e-4c85-8bd8-6ae76fd4f8d5',
    'X1-ORANGE-512',
    'Orange / 512GB',
    '{"storage":"512GB","color":"orange"}',
    '{"storage":"512GB","color":"orange"}',
    1650.00,
    1650.00,
    1820.00,
    1100.00,
    'GHS',
    0,
    0,
    0,
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1
    FROM product_skus
    WHERE id = '5a26c4c7-3b8f-4870-bf88-a4f44a4468c8'
);

UPDATE product_images
SET product_id = '6db6eb8a-c00e-4c85-8bd8-6ae76fd4f8d5',
    storage_key = 'products/zoko-x1-front.png',
    image_url = '/public/uploads/products/zoko-x1-front.png',
    content_type = 'image/png',
    size_bytes = 102400,
    original_filename = 'zoko-x1-front.png',
    sort_order = 1,
    is_primary = TRUE,
    updated_at = CURRENT_TIMESTAMP
WHERE id = 'img-zoko-x1-front-001';

INSERT INTO product_images (
    id,
    product_id,
    storage_key,
    image_url,
    content_type,
    size_bytes,
    original_filename,
    sort_order,
    is_primary,
    created_at,
    updated_at
)
SELECT
    'img-zoko-x1-front-001',
    '6db6eb8a-c00e-4c85-8bd8-6ae76fd4f8d5',
    'products/zoko-x1-front.png',
    '/public/uploads/products/zoko-x1-front.png',
    'image/png',
    102400,
    'zoko-x1-front.png',
    1,
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1
    FROM product_images
    WHERE id = 'img-zoko-x1-front-001'
);

UPDATE product_images
SET product_id = '6db6eb8a-c00e-4c85-8bd8-6ae76fd4f8d5',
    storage_key = 'products/zoko-x1-back.png',
    image_url = '/public/uploads/products/zoko-x1-back.png',
    content_type = 'image/png',
    size_bytes = 112640,
    original_filename = 'zoko-x1-back.png',
    sort_order = 2,
    is_primary = FALSE,
    updated_at = CURRENT_TIMESTAMP
WHERE id = 'img-zoko-x1-back-001';

INSERT INTO product_images (
    id,
    product_id,
    storage_key,
    image_url,
    content_type,
    size_bytes,
    original_filename,
    sort_order,
    is_primary,
    created_at,
    updated_at
)
SELECT
    'img-zoko-x1-back-001',
    '6db6eb8a-c00e-4c85-8bd8-6ae76fd4f8d5',
    'products/zoko-x1-back.png',
    '/public/uploads/products/zoko-x1-back.png',
    'image/png',
    112640,
    'zoko-x1-back.png',
    2,
    FALSE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1
    FROM product_images
    WHERE id = 'img-zoko-x1-back-001'
);
