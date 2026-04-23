package com.zokomart.backend.admin.auth;

import com.zokomart.backend.admin.common.AdminUserStatus;
import com.zokomart.backend.admin.common.AdminUserType;
import com.zokomart.backend.config.AdminBootstrapProperties;
import com.zokomart.backend.config.AdminPasswordConfig.AdminPasswordEncoder;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class AdminBootstrapRunner implements ApplicationRunner {

    private final AdminBootstrapProperties properties;
    private final JdbcTemplate jdbcTemplate;
    private final AdminPasswordEncoder adminPasswordEncoder;

    public AdminBootstrapRunner(
            AdminBootstrapProperties properties,
            JdbcTemplate jdbcTemplate,
            AdminPasswordEncoder adminPasswordEncoder
    ) {
        this.properties = properties;
        this.jdbcTemplate = jdbcTemplate;
        this.adminPasswordEncoder = adminPasswordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.isEnabled()) {
            return;
        }

        String username = required(properties.getUsername(), "zokomart.admin.bootstrap.username");
        String displayName = required(properties.getDisplayName(), "zokomart.admin.bootstrap.display-name");
        String password = required(properties.getPassword(), "zokomart.admin.bootstrap.password");
        String passwordHash = adminPasswordEncoder.encode(password);

        String existingUserId = jdbcTemplate.query(
                "SELECT id FROM admin_users WHERE username = ?",
                resultSet -> resultSet.next() ? resultSet.getString("id") : null,
                username
        );

        if (existingUserId != null) {
            jdbcTemplate.update(
                    """
                            UPDATE admin_users
                            SET display_name = ?,
                                password_hash = ?,
                                user_type = ?,
                                status = ?,
                                updated_at = CURRENT_TIMESTAMP
                            WHERE id = ?
                            """,
                    displayName,
                    passwordHash,
                    AdminUserType.PLATFORM_ADMIN.name(),
                    AdminUserStatus.ACTIVE.name(),
                    existingUserId
            );
            return;
        }

        jdbcTemplate.update(
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
                UUID.randomUUID().toString(),
                username,
                displayName,
                passwordHash,
                AdminUserType.PLATFORM_ADMIN.name(),
                AdminUserStatus.ACTIVE.name()
        );
    }

    private String required(String value, String propertyName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required bootstrap property: " + propertyName);
        }
        return value.trim();
    }
}
