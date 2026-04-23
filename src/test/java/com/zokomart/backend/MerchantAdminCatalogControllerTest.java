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

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MerchantAdminCatalogControllerTest {

    private static final String MERCHANT_ADMIN_ID = "admin-merchant-catalog-001";
    private static final String BOUND_MERCHANT_ID = "c5ce3f6d-3ca0-4b44-84a0-d2bc3f520fa3";
    private static final String CATEGORY_ID = "f6f2c39a-1438-4e90-bcb2-bcb4db719001";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void merchantAdminCanReadCategoryTreeForEditor() throws Exception {
        seedMerchantAdminWithBinding(MERCHANT_ADMIN_ID, BOUND_MERCHANT_ID);

        mockMvc.perform(get("/merchant-admin/categories/tree").cookie(adminSessionCookie(MERCHANT_ADMIN_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(CATEGORY_ID))
                .andExpect(jsonPath("$[0].path").value("/mobile-phones"))
                .andExpect(jsonPath("$[0].children").isArray());
    }

    @Test
    void merchantAdminCanReadResolvedAttributesForEditor() throws Exception {
        seedMerchantAdminWithBinding(MERCHANT_ADMIN_ID, BOUND_MERCHANT_ID);

        mockMvc.perform(get("/merchant-admin/categories/{categoryId}/resolved-attributes", CATEGORY_ID)
                        .cookie(adminSessionCookie(MERCHANT_ADMIN_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryId").value(CATEGORY_ID))
                .andExpect(jsonPath("$.items[0].code").value("screen_size"));
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
                "merchant.catalog.ops." + adminUserId,
                "Merchant Catalog Ops",
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
        return new Cookie("satoken", StpUtil.createLoginSession(adminUserId));
    }
}
