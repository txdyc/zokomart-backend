package com.zokomart.backend;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseMigrationTest {

    @Test
    void flywayMigrationCreatesAdminReadyCommerceTables() {
        String url = "jdbc:h2:mem:adminmigration;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1";
        DriverManagerDataSource dataSource = new DriverManagerDataSource(url, "sa", "");

        Flyway.configure()
                .dataSource(dataSource)
                .locations("filesystem:src/main/resources/db/migration")
                .load()
                .migrate();

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM merchants WHERE merchant_code = 'ZM-SELF-001'", Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM products", Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM categories", Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM product_skus", Integer.class)).isGreaterThanOrEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM carts", Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM cart_items", Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM orders WHERE buyer_id = '00000000-0000-0000-0000-000000000101'", Integer.class)).isGreaterThanOrEqualTo(3);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM order_items", Integer.class)).isGreaterThanOrEqualTo(4);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM payment_intents", Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM order_status_history", Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM fulfillment_records", Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM fulfillment_events", Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM admin_users", Integer.class)).isGreaterThanOrEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM admin_user_merchants", Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT user_type FROM admin_users WHERE username = 'platform.root'", String.class))
                .isEqualTo("PLATFORM_ADMIN");
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM admin_action_logs", Integer.class)).isZero();
        assertThat(columnExists(jdbcTemplate, "products", "brand_id")).isTrue();
        assertThat(columnExists(jdbcTemplate, "products", "description_html")).isTrue();
        assertThat(columnExists(jdbcTemplate, "products", "attributes_json")).isTrue();
        assertThat(columnExists(jdbcTemplate, "products", "deleted_at")).isTrue();
        assertThat(columnExists(jdbcTemplate, "product_skus", "spu_id")).isTrue();
        assertThat(columnExists(jdbcTemplate, "product_skus", "specs_json")).isTrue();
        assertThat(columnExists(jdbcTemplate, "product_skus", "price")).isTrue();
        assertThat(columnExists(jdbcTemplate, "product_skus", "stock")).isTrue();
        assertThat(columnExists(jdbcTemplate, "product_skus", "locked_stock")).isTrue();
        assertThat(columnExists(jdbcTemplate, "product_skus", "deleted_at")).isTrue();
        assertThat(columnExists(jdbcTemplate, "categories", "parent_id")).isTrue();
        assertThat(columnExists(jdbcTemplate, "categories", "path")).isTrue();
        assertThat(columnExists(jdbcTemplate, "brands", "image_url")).isTrue();
        assertThat(tableExists(jdbcTemplate, "brands")).isTrue();
        assertThat(tableExists(jdbcTemplate, "product_images")).isTrue();
        assertThat(tableExists(jdbcTemplate, "attributes")).isTrue();
        assertThat(tableExists(jdbcTemplate, "product_attribute_values")).isTrue();
        assertThat(tableExists(jdbcTemplate, "merchant_custom_attributes")).isTrue();
        assertThat(tableExists(jdbcTemplate, "inventory_lock_records")).isTrue();
        assertThat(tableExists(jdbcTemplate, "buyer_profiles")).isTrue();
        assertThat(tableExists(jdbcTemplate, "buyer_wallet_accounts")).isTrue();
        assertThat(tableExists(jdbcTemplate, "buyer_transactions")).isTrue();
        assertThat(tableExists(jdbcTemplate, "buyer_saved_addresses")).isTrue();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM buyer_profiles WHERE buyer_id = '00000000-0000-0000-0000-000000000101'", Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM merchants WHERE merchant_code = 'ZM-SELF-001'", String.class)).isEqualTo("APPROVED");
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM products LIMIT 1", String.class)).isEqualTo("APPROVED");
        assertThat(jdbcTemplate.queryForObject("SELECT category_id FROM products LIMIT 1", String.class))
                .isEqualTo("f6f2c39a-1438-4e90-bcb2-bcb4db719001");
    }

    private boolean columnExists(JdbcTemplate jdbcTemplate, String tableName, String columnName) {
        Integer count = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM information_schema.columns
                        WHERE lower(table_name) = lower(?)
                          AND lower(column_name) = lower(?)
                        """,
                Integer.class,
                tableName,
                columnName
        );
        return count != null && count > 0;
    }

    private boolean tableExists(JdbcTemplate jdbcTemplate, String tableName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE lower(table_name) = lower(?)",
                Integer.class,
                tableName
        );
        return count != null && count > 0;
    }
}
