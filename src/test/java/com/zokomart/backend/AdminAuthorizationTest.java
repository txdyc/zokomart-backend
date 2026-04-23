package com.zokomart.backend;

import cn.dev33.satoken.stp.StpUtil;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminAuthorizationTest {

    private static final String MERCHANT_ID = "c5ce3f6d-3ca0-4b44-84a0-d2bc3f520fa3";
    private static final String PLATFORM_ADMIN_ID = "admin-platform-001";
    private static final String MERCHANT_ADMIN_ID = "admin-merchant-001";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void createMerchantAdminRequiresPlatformAdmin() throws Exception {
        mockMvc.perform(post("/admin/users")
                        .cookie(loginAsMerchantAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validMerchantAdminCreateRequest("merchant.kumasi.ops", "Kumasi Merchant Ops")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail.code").value("ADMIN_FORBIDDEN"));
    }

    @Test
    void platformAdminCannotUseMerchantWriteApi() throws Exception {
        mockMvc.perform(multipart("/merchant-admin/products")
                        .file(image("front.png"))
                        .cookie(loginAsPlatformAdmin())
                        .param("payload", validMerchantProductCreateRequest())
                        .param("newImageClientIds", "new-front")
                        .param("imageOrder", "new:new-front"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail.code").value("ADMIN_FORBIDDEN"));
    }

    private Cookie loginAsMerchantAdmin() throws Exception {
        seedAdminUser(MERCHANT_ADMIN_ID, "merchant.accra.ops", "Accra Merchant Ops", "MERCHANT_ADMIN");
        jdbcTemplate.update(
                """
                        INSERT INTO admin_user_merchants (id, admin_user_id, merchant_id, created_at)
                        VALUES (?, ?, ?, CURRENT_TIMESTAMP)
                        """,
                "admin-merchant-binding-001",
                MERCHANT_ADMIN_ID,
                MERCHANT_ID
        );
        return adminSessionCookie(MERCHANT_ADMIN_ID);
    }

    private Cookie loginAsPlatformAdmin() {
        seedAdminUser(PLATFORM_ADMIN_ID, "platform.review.ops", "Platform Root", "PLATFORM_ADMIN");
        return adminSessionCookie(PLATFORM_ADMIN_ID);
    }

    private void seedAdminUser(String id, String username, String displayName, String userType) {
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
                "{noop}not-used-in-this-test",
                userType,
                "ACTIVE"
        );
    }

    private Cookie adminSessionCookie(String adminUserId) {
        String token = StpUtil.createLoginSession(adminUserId);
        return new Cookie("satoken", token);
    }

    private String validMerchantAdminCreateRequest(String username, String displayName) {
        return """
                {
                  "username": "%s",
                  "displayName": "%s",
                  "password": "Passw0rd!",
                  "userType": "MERCHANT_ADMIN",
                  "merchantIds": ["%s"]
                }
                """.formatted(username, displayName, MERCHANT_ID);
    }

    private String validMerchantProductCreateRequest() {
        return """
                {
                  "merchantId": "%s",
                  "productCode": "PRD-MERCHANT-NEW-001",
                  "name": "Merchant Test Phone",
                  "description": "New merchant-managed smartphone listing",
                  "categoryId": "f6f2c39a-1438-4e90-bcb2-bcb4db719001",
                  "skus": [
                    {
                      "skuCode": "SKU-MERCHANT-NEW-001",
                      "skuName": "Black / 128GB",
                      "attributesJson": "{\\"color\\":\\"black\\",\\"storage\\":\\"128GB\\"}",
                      "unitPriceAmount": 1250.00,
                      "currencyCode": "GHS",
                      "availableQuantity": 12
                    }
                  ]
                }
                """.formatted(MERCHANT_ID);
    }

    private MockMultipartFile image(String filename) {
        return new MockMultipartFile("newImages", filename, "image/png", new byte[] {1, 2, 3});
    }
}
