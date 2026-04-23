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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminProductControllerTest {

    private static final String PLATFORM_ADMIN_ID = "admin-platform-product-ops-001";
    private static final String PRODUCT_ID = "6db6eb8a-c00e-4c85-8bd8-6ae76fd4f8d5";
    private static final String PENDING_PRODUCT_ID = "85f74643-a6b2-4d32-a69a-7ca9de0c9101";
    private static final String INACTIVE_PRODUCT_ID = "66ce3b2c-f6ff-4c50-8395-a24e20835555";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void listAdminProductsRequiresAuthenticatedPlatformAdmin() throws Exception {
        mockMvc.perform(get("/admin/products"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail.code").value("ADMIN_UNAUTHORIZED"));
    }

    @Test
    void listAdminProductsReturnsSeededCatalog() throws Exception {
        seedProductImage(PRODUCT_ID, "/public/uploads/products/phone-front.png", "products/phone-front.png", 0, true);

        mockMvc.perform(get("/admin/products").cookie(loginAsPlatformAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(PRODUCT_ID))
                .andExpect(jsonPath("$.items[0].status").value("APPROVED"))
                .andExpect(jsonPath("$.items[0].category.name").value("Mobile Phones"))
                .andExpect(jsonPath("$.items[0].priceAmount").value("1250.00"))
                .andExpect(jsonPath("$.items[0].thumbnailUrl").isNotEmpty());
    }

    @Test
    void getAdminProductDetailReturnsMerchantCategoryAndSkuState() throws Exception {
        seedProductImage(PRODUCT_ID, "/public/uploads/products/phone-front.png", "products/phone-front.png", 0, true);

        mockMvc.perform(get("/admin/products/{productId}", PRODUCT_ID).cookie(loginAsPlatformAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(PRODUCT_ID))
                .andExpect(jsonPath("$.merchant.status").value("APPROVED"))
                .andExpect(jsonPath("$.category.status").value("ACTIVE"))
                .andExpect(jsonPath("$.skus[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.thumbnailUrl").isNotEmpty())
                .andExpect(jsonPath("$.images[0].isPrimary").value(true));
    }

    @Test
    void platformAdminCanApprovePendingProduct() throws Exception {
        insertPendingReviewProduct();

        mockMvc.perform(
                        post("/admin/products/{id}/approve", PENDING_PRODUCT_ID)
                                .cookie(loginAsPlatformAdmin())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "reason": "QA passed"
                                        }
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(PENDING_PRODUCT_ID))
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void platformAdminCanRejectPendingProduct() throws Exception {
        insertPendingReviewProduct();

        mockMvc.perform(
                        post("/admin/products/{id}/reject", PENDING_PRODUCT_ID)
                                .cookie(loginAsPlatformAdmin())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "reason": "No longer allowed"
                                        }
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(PENDING_PRODUCT_ID))
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    void platformAdminCanDeactivateApprovedProduct() throws Exception {
        mockMvc.perform(
                        post("/admin/products/{id}/deactivate", PRODUCT_ID)
                                .cookie(loginAsPlatformAdmin())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "reason": "Temporarily disabled after QA review"
                                        }
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(PRODUCT_ID))
                .andExpect(jsonPath("$.status").value("INACTIVE"));
    }

    @Test
    void platformAdminCanReactivateInactiveProduct() throws Exception {
        insertInactiveProduct();

        mockMvc.perform(
                        post("/admin/products/{id}/reactivate", INACTIVE_PRODUCT_ID)
                                .cookie(loginAsPlatformAdmin())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "reason": "Manual reactivation"
                                        }
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(INACTIVE_PRODUCT_ID))
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    private Cookie loginAsPlatformAdmin() {
        seedAdminUser(PLATFORM_ADMIN_ID, "platform.review.product", "Platform Product Reader", "PLATFORM_ADMIN");
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

    private void insertPendingReviewProduct() {
        insertProductWithStatus(PENDING_PRODUCT_ID, "PRD-PENDING-001", "Pending Phone Review", "PENDING_REVIEW");
    }

    private void insertInactiveProduct() {
        insertProductWithStatus(INACTIVE_PRODUCT_ID, "PRD-INACTIVE-001", "Inactive Phone Review", "INACTIVE");
    }

    private void insertProductWithStatus(String productId, String productCode, String name, String statusValue) {
        jdbcTemplate.update(
                """
                        INSERT INTO products (id, merchant_id, product_code, name, description, status, is_self_operated, category_id, created_at, updated_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """,
                productId,
                "c5ce3f6d-3ca0-4b44-84a0-d2bc3f520fa3",
                productCode,
                name,
                "Admin managed product",
                statusValue,
                true,
                "f6f2c39a-1438-4e90-bcb2-bcb4db719001"
        );
        jdbcTemplate.update(
                """
                        INSERT INTO product_skus (id, product_id, sku_code, sku_name, attributes_json, unit_price_amount, currency_code, available_quantity, status, created_at, updated_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        """,
                java.util.UUID.randomUUID().toString(),
                productId,
                productCode + "-BLK-64",
                "黑色 / 64GB",
                "{\"color\":\"black\",\"storage\":\"64GB\"}",
                999.00,
                "GHS",
                12,
                "ACTIVE"
        );
    }

    private void seedProductImage(String productId, String imageUrl, String storageKey, int sortOrder, boolean isPrimary) {
        jdbcTemplate.update(
                """
                        INSERT INTO product_images (
                            id,
                            product_id,
                            storage_key,
                            image_url,
                            content_type,
                            size_bytes,
                            original_filename,
                            sort_order,
                            is_primary,
                            created_at,
                            updated_at
                        )
                        VALUES (?, ?, ?, ?, 'image/png', 32, 'seed.png', ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        """,
                java.util.UUID.randomUUID().toString(),
                productId,
                storageKey,
                imageUrl,
                sortOrder,
                isPrimary
        );
    }
}
