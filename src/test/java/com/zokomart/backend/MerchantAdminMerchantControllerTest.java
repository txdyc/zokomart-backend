package com.zokomart.backend;

import cn.dev33.satoken.stp.StpUtil;
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

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MerchantAdminMerchantControllerTest {

    private static final String MERCHANT_ADMIN_ID = "admin-merchant-scope-merchant-001";
    private static final String BOUND_MERCHANT_ID = "c5ce3f6d-3ca0-4b44-84a0-d2bc3f520fa3";
    private static final String UNBOUND_MERCHANT_ID = "d7a6d1f6-7ab2-45d0-bf96-2f2f7c07f999";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void merchantAdminOnlySeesBoundMerchants() throws Exception {
        seedMerchantAdminWithBinding(MERCHANT_ADMIN_ID, BOUND_MERCHANT_ID);

        mockMvc.perform(get("/merchant-admin/merchants").cookie(adminSessionCookie(MERCHANT_ADMIN_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].id").value(BOUND_MERCHANT_ID));
    }

    @Test
    void merchantAdminCannotAccessUnboundMerchantDetail() throws Exception {
        seedMerchantAdminWithBinding(MERCHANT_ADMIN_ID, BOUND_MERCHANT_ID);

        mockMvc.perform(get("/merchant-admin/merchants/{merchantId}", UNBOUND_MERCHANT_ID)
                        .cookie(adminSessionCookie(MERCHANT_ADMIN_ID)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail.code").value("MERCHANT_SCOPE_FORBIDDEN"));
    }

    @Test
    void merchantAdminCanUpdateBoundMerchantDisplayName() throws Exception {
        seedMerchantAdminWithBinding(MERCHANT_ADMIN_ID, BOUND_MERCHANT_ID);

        mockMvc.perform(put("/merchant-admin/merchants/{merchantId}", BOUND_MERCHANT_ID)
                        .cookie(adminSessionCookie(MERCHANT_ADMIN_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName": "Accra Mobile Hub Plus"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.merchant.id").value(BOUND_MERCHANT_ID))
                .andExpect(jsonPath("$.merchant.displayName").value("Accra Mobile Hub Plus"));
    }

    private void seedMerchantAdminWithBinding(String adminUserId, String merchantId) {
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
                        VALUES (?, ?, ?, ?, 'MERCHANT_ADMIN', 'ACTIVE', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        """,
                adminUserId,
                "merchant.scope.ops." + adminUserId,
                "Merchant Scope Ops",
                "{noop}not-used-in-this-test"
        );
        jdbcTemplate.update(
                """
                        INSERT INTO admin_user_merchants (id, admin_user_id, merchant_id, created_at)
                        VALUES (?, ?, ?, CURRENT_TIMESTAMP)
                        """,
                UUID.randomUUID().toString(),
                adminUserId,
                merchantId
        );
    }

    private Cookie adminSessionCookie(String adminUserId) {
        String token = StpUtil.createLoginSession(adminUserId);
        return new Cookie("satoken", token);
    }
}
