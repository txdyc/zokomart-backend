package com.zokomart.backend.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "zokomart.admin.bootstrap.enabled=true",
        "zokomart.admin.bootstrap.username=platform.root",
        "zokomart.admin.bootstrap.display-name=Platform Bootstrap Test",
        "zokomart.admin.bootstrap.password=ChangedPassw0rd!"
})
@ActiveProfiles("test")
class AdminBootstrapRunnerTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AdminPasswordConfig.AdminPasswordEncoder adminPasswordEncoder;

    @Test
    void bootstrapRunnerReconcilesSeededPlatformRootUserWhenEnabled() {
        String displayName = jdbcTemplate.queryForObject(
                "SELECT display_name FROM admin_users WHERE username = 'platform.root'",
                String.class
        );
        String passwordHash = jdbcTemplate.queryForObject(
                "SELECT password_hash FROM admin_users WHERE username = 'platform.root'",
                String.class
        );

        assertThat(displayName).isEqualTo("Platform Bootstrap Test");
        assertThat(passwordHash).isNotBlank();
        assertThat(adminPasswordEncoder.matches("ChangedPassw0rd!", passwordHash)).isTrue();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM admin_users WHERE username = 'platform.root'",
                Integer.class
        )).isEqualTo(1);
    }
}
