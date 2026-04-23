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
class AdminAuthControllerTest {

    private static final String PASSWORD_HASH_FOR_PASSW0RD = "pbkdf2-sha256$310000$QWRtaW4tU2VlZC0yMDI2IQ==$cEFKomdOKsSly0xDaWVmRtTCPIx7HaHKR+8AYWwrwNo=";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void loginReturnsSessionAndRole() throws Exception {
        mockMvc.perform(post("/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "platform.root",
                                  "password": "Passw0rd!"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("satoken"))
                .andExpect(jsonPath("$.user.username").value("platform.root"))
                .andExpect(jsonPath("$.user.userType").value("PLATFORM_ADMIN"))
                .andExpect(jsonPath("$.user.merchantBindings").isArray());
    }

    @Test
    void currentUserReturnsAuthenticatedPlatformAdmin() throws Exception {
        mockMvc.perform(get("/admin/auth/me").cookie(loginAsPlatformAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("platform.root"))
                .andExpect(jsonPath("$.displayName").value("Platform Root"))
                .andExpect(jsonPath("$.userType").value("PLATFORM_ADMIN"))
                .andExpect(jsonPath("$.merchantBindings").isArray());
    }

    @Test
    void logoutClearsCurrentSession() throws Exception {
        Cookie sessionCookie = loginAsPlatformAdmin();

        mockMvc.perform(post("/admin/auth/logout").cookie(sessionCookie))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/admin/auth/me").cookie(sessionCookie))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail.code").value("ADMIN_UNAUTHORIZED"));
    }

    @Test
    void disabledUserCannotLogin() throws Exception {
        seedAdminUser("admin-disabled-001", "platform.disabled.ops", "Disabled Ops", "DISABLED");

        mockMvc.perform(post("/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "platform.disabled.ops",
                                  "password": "Passw0rd!"
                                }
                                """))
                .andExpect(status().isUnauthorized());
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

    private void seedAdminUser(String id, String username, String displayName, String status) {
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
                "PLATFORM_ADMIN",
                status
        );
    }
}
