package com.zokomart.backend;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ProductDomainMigrationTest {

    @Test
    void flywayMigrationAddsProductDomainPlatformizationSchema() throws SQLException {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:productdomainmigration;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1",
                "sa",
                ""
        );

        Flyway.configure()
                .dataSource(dataSource)
                .locations("filesystem:src/main/resources/db/migration")
                .load()
                .migrate();

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        assertThat(columnNames(jdbcTemplate, "products")).contains(
                "brand_id",
                "description_html",
                "attributes_json",
                "deleted_at"
        );
        assertThat(columnNames(jdbcTemplate, "product_skus")).contains(
                "spu_id",
                "specs_json",
                "price",
                "original_price",
                "cost_price",
                "stock",
                "locked_stock",
                "deleted_at"
        );
        assertThat(columnNames(jdbcTemplate, "categories")).contains(
                "parent_id",
                "code",
                "path",
                "level",
                "sort_order",
                "deleted_at"
        );
        assertThat(indexNames(jdbcTemplate, "products")).contains(
                "idx_products_category_id",
                "idx_products_attributes_gin"
        );
        assertThat(indexNames(jdbcTemplate, "product_skus")).contains("idx_product_skus_price");
        assertThat(tableExists(jdbcTemplate, "brands")).isTrue();
        assertThat(tableExists(jdbcTemplate, "attributes")).isTrue();
        assertThat(tableExists(jdbcTemplate, "product_attribute_values")).isTrue();
        assertThat(tableExists(jdbcTemplate, "merchant_custom_attributes")).isTrue();
        assertThat(tableExists(jdbcTemplate, "inventory_lock_records")).isTrue();
    }

    private Set<String> columnNames(JdbcTemplate jdbcTemplate, String tableName) {
        return new HashSet<>(jdbcTemplate.queryForList(
                """
                        SELECT lower(column_name)
                        FROM information_schema.columns
                        WHERE lower(table_name) = lower(?)
                        """,
                String.class,
                tableName
        ));
    }

    private Set<String> indexNames(JdbcTemplate jdbcTemplate, String tableName) {
        return new HashSet<>(jdbcTemplate.queryForList(
                """
                        SELECT lower(index_name)
                        FROM information_schema.indexes
                        WHERE lower(table_name) = lower(?)
                        """,
                String.class,
                tableName
        ));
    }

    private boolean tableExists(JdbcTemplate jdbcTemplate, String tableName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE lower(table_name) = ?",
                Integer.class,
                tableName.toLowerCase(Locale.ROOT)
        );
        return count != null && count > 0;
    }
}
