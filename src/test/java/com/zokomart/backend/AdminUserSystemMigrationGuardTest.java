package com.zokomart.backend;

import com.zokomart.backend.config.AdminPasswordConfig;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;

class AdminUserSystemMigrationGuardTest {

    @Test
    void flywayMigrationEnforcesUniqueAdminUsername() {
        String url = "jdbc:h2:mem:adminmigration-unique;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1";
        DriverManagerDataSource dataSource = new DriverManagerDataSource(url, "sa", "");

        Flyway.configure()
                .dataSource(dataSource)
                .locations("filesystem:src/main/resources/db/migration")
                .load()
                .migrate();

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        assertThatThrownBy(() -> jdbcTemplate.update(
                """
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
                        VALUES (?, ?, ?, ?, ?, ?, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        """,
                "duplicate-admin-user",
                "platform.root",
                "Duplicate Platform Root",
                "pbkdf2-sha256$310000$Wm9rb01hcnQtQWRtaW4tQg==$DWjoiZQWVMEKN4+xDGVmokSyaHxEOlpAmuGOhvi/NTc=",
                "PLATFORM_ADMIN",
                "ACTIVE"
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void flywayMigrationSeedsPlatformRootWithNonLocalPasswordHash() {
        String url = "jdbc:h2:mem:adminmigration-placeholder;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1";
        DriverManagerDataSource dataSource = new DriverManagerDataSource(url, "sa", "");

        Flyway.configure()
                .dataSource(dataSource)
                .locations("filesystem:src/main/resources/db/migration")
                .load()
                .migrate();

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        String passwordHash = jdbcTemplate.queryForObject(
                "SELECT password_hash FROM admin_users WHERE username = 'platform.root'",
                String.class
        );

        AdminPasswordConfig.AdminPasswordEncoder encoder = new AdminPasswordConfig().adminPasswordEncoder();
        assertThat(encoder.matches("Passw0rd!", passwordHash)).isFalse();
    }
}
