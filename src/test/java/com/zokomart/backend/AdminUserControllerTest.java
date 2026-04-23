package com.zokomart.backend;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminUserControllerTest {

    private static final String MERCHANT_ID = "c5ce3f6d-3ca0-4b44-84a0-d2bc3f520fa3";
    private static final String PASSWORD_HASH_FOR_PASSW0RD = "pbkdf2-sha256$310000$QWRtaW4tU2VlZC0yMDI2IQ==$cEFKomdOKsSly0xDaWVmRtTCPIx7HaHKR+8AYWwrwNo=";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void platformAdminCanCreatePlatformAdminUser() throws Exception {
        mockMvc.perform(post("/admin/users")
                        .cookie(loginAsPlatformAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPlatformAdminCreateRequest()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("platform.ops.audit"))
                .andExpect(jsonPath("$.displayName").value("Platform Ops Audit"))
                .andExpect(jsonPath("$.userType").value("PLATFORM_ADMIN"))
                .andExpect(jsonPath("$.merchantBindings").isArray())
                .andExpect(jsonPath("$.merchantBindings.length()").value(0));

        Integer actionLogCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM admin_action_logs WHERE entity_type = 'ADMIN_USER' AND action_code = 'CREATE_ADMIN_USER'",
                Integer.class
        );
        assertThat(actionLogCount).isEqualTo(1);
    }

    @Test
    void platformAdminCanListAndGetUserDetails() throws Exception {
        seedAdminUser("admin-platform-ops-001", "platform.ops.reader", "Platform Ops Reader", "PLATFORM_ADMIN", "ACTIVE");

        mockMvc.perform(get("/admin/users").cookie(loginAsPlatformAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items[?(@.id == 'admin-platform-ops-001')].username").value("platform.ops.reader"));

        mockMvc.perform(get("/admin/users/{userId}", "admin-platform-ops-001").cookie(loginAsPlatformAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("admin-platform-ops-001"))
                .andExpect(jsonPath("$.username").value("platform.ops.reader"))
                .andExpect(jsonPath("$.displayName").value("Platform Ops Reader"))
                .andExpect(jsonPath("$.userType").value("PLATFORM_ADMIN"))
                .andExpect(jsonPath("$.merchantBindings.length()").value(0));
    }

    @Test
    void platformAdminCanDisableAndEnableAdminUser() throws Exception {
        seedAdminUser("admin-platform-toggle-001", "platform.ops.toggle", "Platform Ops Toggle", "PLATFORM_ADMIN", "ACTIVE");

        mockMvc.perform(post("/admin/users/{userId}/disable", "admin-platform-toggle-001")
                        .cookie(loginAsPlatformAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("admin-platform-toggle-001"))
                .andExpect(jsonPath("$.status").value("DISABLED"));

        mockMvc.perform(post("/admin/users/{userId}/enable", "admin-platform-toggle-001")
                        .cookie(loginAsPlatformAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("admin-platform-toggle-001"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        Integer actionLogCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM admin_action_logs WHERE entity_type = 'ADMIN_USER' AND entity_id = 'admin-platform-toggle-001'",
                Integer.class
        );
        assertThat(actionLogCount).isEqualTo(2);
    }

    @Test
    void platformAdminCannotDisableSelf() throws Exception {
        mockMvc.perform(post("/admin/users/{userId}/disable", "00000000-0000-0000-0000-000000000001")
                        .cookie(loginAsPlatformAdmin()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail.code").value("ADMIN_USER_SELF_DISABLE_FORBIDDEN"));

        String statusValue = jdbcTemplate.queryForObject(
                "SELECT status FROM admin_users WHERE id = ?",
                String.class,
                "00000000-0000-0000-0000-000000000001"
        );
        assertThat(statusValue).isEqualTo("ACTIVE");
    }

    @Test
    void merchantAdminRequiresAtLeastOneMerchantBinding() throws Exception {
        mockMvc.perform(post("/admin/users")
                        .cookie(loginAsPlatformAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "merchant.empty.binding",
                                  "displayName": "Merchant Empty Binding",
                                  "password": "Passw0rd!",
                                  "userType": "MERCHANT_ADMIN",
                                  "merchantIds": []
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail.code").value("MERCHANT_ADMIN_BINDINGS_REQUIRED"));
    }

    @Test
    void platformAdminCannotBeCreatedWithMerchantBindings() throws Exception {
        mockMvc.perform(post("/admin/users")
                        .cookie(loginAsPlatformAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "platform.invalid.binding",
                                  "displayName": "Platform Invalid Binding",
                                  "password": "Passw0rd!",
                                  "userType": "PLATFORM_ADMIN",
                                  "merchantIds": ["%s"]
                                }
                                """.formatted(MERCHANT_ID)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail.code").value("PLATFORM_ADMIN_BINDINGS_NOT_ALLOWED"));
    }

    @Test
    void platformAdminCanUpdateMerchantBindingsForMerchantAdmin() throws Exception {
        seedAdminUser("admin-merchant-bind-001", "merchant.bind.ops", "Merchant Bind Ops", "MERCHANT_ADMIN", "ACTIVE");

        mockMvc.perform(post("/admin/users/{userId}/merchant-bindings", "admin-merchant-bind-001")
                        .cookie(loginAsPlatformAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "merchantIds": ["%s"]
                                }
                                """.formatted(MERCHANT_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("admin-merchant-bind-001"))
                .andExpect(jsonPath("$.userType").value("MERCHANT_ADMIN"))
                .andExpect(jsonPath("$.merchantBindings.length()").value(1))
                .andExpect(jsonPath("$.merchantBindings[0].id").value(MERCHANT_ID));

        Integer actionLogCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM admin_action_logs WHERE entity_type = 'ADMIN_USER_MERCHANT_BINDING' AND entity_id = 'admin-merchant-bind-001'",
                Integer.class
        );
        assertThat(actionLogCount).isEqualTo(1);
    }

    @Test
    void disablingUserInvalidatesAlreadyIssuedSessionCookie() throws Exception {
        Cookie platformRootCookie = loginAsPlatformAdmin();

        mockMvc.perform(post("/admin/users")
                        .cookie(platformRootCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "platform.session.ops",
                                  "displayName": "Platform Session Ops",
                                  "password": "Passw0rd!",
                                  "userType": "PLATFORM_ADMIN",
                                  "merchantIds": []
                                }
                                """))
                .andExpect(status().isCreated());

        String targetUserId = jdbcTemplate.queryForObject(
                "SELECT id FROM admin_users WHERE username = ?",
                String.class,
                "platform.session.ops"
        );
        assertThat(targetUserId).isNotBlank();

        Cookie targetUserCookie = mockMvc.perform(post("/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "platform.session.ops",
                                  "password": "Passw0rd!"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("satoken"))
                .andReturn()
                .getResponse()
                .getCookie("satoken");

        assertThat(targetUserCookie).isNotNull();

        mockMvc.perform(post("/admin/users/{userId}/disable", targetUserId)
                        .cookie(platformRootCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISABLED"));

        mockMvc.perform(get("/admin/auth/me").cookie(targetUserCookie))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail.code").value("ADMIN_SESSION_INVALID"));
    }

    @Test
    void creatingUserWithDuplicateUsernameReturnsConflict() throws Exception {
        mockMvc.perform(post("/admin/users")
                        .cookie(loginAsPlatformAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "platform.root",
                                  "displayName": "Duplicate Platform Root",
                                  "password": "Passw0rd!",
                                  "userType": "PLATFORM_ADMIN",
                                  "merchantIds": []
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail.code").value("ADMIN_USERNAME_ALREADY_EXISTS"));
    }

    @Test
    void creatingMerchantAdminWithNonexistentMerchantReturnsNotFound() throws Exception {
        mockMvc.perform(post("/admin/users")
                        .cookie(loginAsPlatformAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "merchant.missing.binding",
                                  "displayName": "Merchant Missing Binding",
                                  "password": "Passw0rd!",
                                  "userType": "MERCHANT_ADMIN",
                                  "merchantIds": ["merchant-missing-001"]
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail.code").value("MERCHANT_NOT_FOUND"));
    }

    private Cookie loginAsPlatformAdmin() throws Exception {
        Cookie sessionCookie = mockMvc.perform(post("/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "platform.root",
                                  "password": "Passw0rd!"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("satoken"))
                .andReturn()
                .getResponse()
                .getCookie("satoken");

        assertThat(sessionCookie).isNotNull();
        return sessionCookie;
    }

    private String validPlatformAdminCreateRequest() {
        return """
                {
                  "username": "platform.ops.audit",
                  "displayName": "Platform Ops Audit",
                  "password": "Passw0rd!",
                  "userType": "PLATFORM_ADMIN",
                  "merchantIds": []
                }
                """;
    }

    private void seedAdminUser(String id, String username, String displayName, String userType, String status) {
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
                id,
                username,
                displayName,
                PASSWORD_HASH_FOR_PASSW0RD,
                userType,
                status
        );
    }
}
