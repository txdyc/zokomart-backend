INSERT INTO carts (id, buyer_id, status, created_at, updated_at)
VALUES ('d7b9fb04-4f81-4ea5-918f-21a86ff72a8d', '00000000-0000-0000-0000-000000000101', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO cart_items (id, cart_id, merchant_id, product_id, sku_id, product_name_snapshot, sku_name_snapshot, reference_price_amount, currency_code, quantity, created_at, updated_at)
VALUES ('1a8ffb74-6b67-4638-b73a-4b15c6bc866a', 'd7b9fb04-4f81-4ea5-918f-21a86ff72a8d', 'c5ce3f6d-3ca0-4b44-84a0-d2bc3f520fa3', '6db6eb8a-c00e-4c85-8bd8-6ae76fd4f8d5', '77cfce71-f8f5-44d5-9adc-0a76d5f65d5a', 'Zoko Phone X1', '黑色 / 128GB', 1250.00, 'GHS', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
