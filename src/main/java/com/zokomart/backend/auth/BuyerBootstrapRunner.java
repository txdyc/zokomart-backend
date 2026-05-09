package com.zokomart.backend.auth;

import com.zokomart.backend.config.BuyerBootstrapProperties;
import com.zokomart.backend.auth.BuyerPasswordConfig.BuyerPasswordEncoder;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class BuyerBootstrapRunner implements ApplicationRunner {

    private final BuyerBootstrapProperties properties;
    private final JdbcTemplate jdbcTemplate;
    private final BuyerPasswordEncoder buyerPasswordEncoder;

    public BuyerBootstrapRunner(
            BuyerBootstrapProperties properties,
            JdbcTemplate jdbcTemplate,
            BuyerPasswordEncoder buyerPasswordEncoder
    ) {
        this.properties = properties;
        this.jdbcTemplate = jdbcTemplate;
        this.buyerPasswordEncoder = buyerPasswordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.isEnabled()) {
            return;
        }

        String buyerId = required(properties.getBuyerId(), "zokomart.buyer.bootstrap.buyer-id");
        String phoneNumber = required(properties.getPhoneNumber(), "zokomart.buyer.bootstrap.phone-number");
        String password = required(properties.getPassword(), "zokomart.buyer.bootstrap.password");
        String normalizedPhoneNumber = BuyerPhoneNumberNormalizer.normalize(phoneNumber);
        String passwordHash = buyerPasswordEncoder.encode(password);

        Integer buyerProfileCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM buyer_profiles WHERE buyer_id = ?",
                Integer.class,
                buyerId
        );
        if (buyerProfileCount == null || buyerProfileCount == 0) {
            throw new IllegalStateException("Missing buyer profile for bootstrap buyer id: " + buyerId);
        }

        Integer existingAccountCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM buyer_auth_accounts WHERE buyer_id = ?",
                Integer.class,
                buyerId
        );

        if (existingAccountCount != null && existingAccountCount > 0) {
            jdbcTemplate.update(
                    """
                            UPDATE buyer_auth_accounts
                            SET phone_number = ?,
                                phone_number_normalized = ?,
                                password_hash = ?,
                                status = ?,
                                password_updated_at = CURRENT_TIMESTAMP,
                                updated_at = CURRENT_TIMESTAMP
                            WHERE buyer_id = ?
                            """,
                    phoneNumber,
                    normalizedPhoneNumber,
                    passwordHash,
                    BuyerAuthAccountStatus.ACTIVE.name(),
                    buyerId
            );
            return;
        }

        jdbcTemplate.update(
                """
                        INSERT INTO buyer_auth_accounts (
                            buyer_id,
                            phone_number,
                            phone_number_normalized,
                            password_hash,
                            status,
                            last_login_at,
                            password_updated_at,
                            created_at,
                            updated_at
                        )
                        VALUES (?, ?, ?, ?, ?, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        """,
                buyerId,
                phoneNumber,
                normalizedPhoneNumber,
                passwordHash,
                BuyerAuthAccountStatus.ACTIVE.name()
        );
    }

    private String required(String value, String propertyName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required bootstrap property: " + propertyName);
        }
        return value.trim();
    }
}
