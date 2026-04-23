package com.zokomart.backend;

import cn.dev33.satoken.stp.StpUtil;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminDashboardControllerTest {

    private static final String PLATFORM_ADMIN_ID = "admin-dashboard-ops-001";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void dashboardRequiresAuthenticatedPlatformAdmin() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail.code").value("ADMIN_UNAUTHORIZED"));
    }

    @Test
    void dashboardReturnsOpsStatsAndActionItems() throws Exception {
        mockMvc.perform(get("/admin/dashboard").cookie(loginAsPlatformAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stats.activeProducts").value(1))
                .andExpect(jsonPath("$.stats.pendingPaymentOrders").value(0))
                .andExpect(jsonPath("$.actionItems[0].type").value("PENDING_PRODUCT_REVIEW"))
                .andExpect(jsonPath("$.actionItems[0].href").value("/products?status=PENDING_REVIEW"));
    }

    @Test
    void dashboardCorsPreflightAllowsAdminDevOrigin() throws Exception {
        mockMvc.perform(options("/admin/dashboard")
                        .header("Origin", "http://localhost:3001")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "X-Admin-Id, Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3001"));
    }

    private Cookie loginAsPlatformAdmin() {
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
                PLATFORM_ADMIN_ID,
                "platform.dashboard.ops",
                "Platform Dashboard Ops",
                "{noop}not-used-in-this-test",
                "PLATFORM_ADMIN",
                "ACTIVE"
        );
        return new Cookie("satoken", StpUtil.createLoginSession(PLATFORM_ADMIN_ID));
    }
}
