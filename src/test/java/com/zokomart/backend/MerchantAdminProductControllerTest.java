package com.zokomart.backend;

import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MerchantAdminProductControllerTest {

    private static final String MERCHANT_ADMIN_ID = "admin-merchant-scope-product-001";
    private static final String BOUND_MERCHANT_ID = "c5ce3f6d-3ca0-4b44-84a0-d2bc3f520fa3";
    private static final String UNBOUND_MERCHANT_ID = "d7a6d1f6-7ab2-45d0-bf96-2f2f7c07f999";
    private static final String SEEDED_PRODUCT_ID = "6db6eb8a-c00e-4c85-8bd8-6ae76fd4f8d5";
    private static final String SEEDED_SKU_ID = "77cfce71-f8f5-44d5-9adc-0a76d5f65d5a";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void merchantAdminOnlySeesBoundMerchantProducts() throws Exception {
        seedMerchantAdminWithBinding(MERCHANT_ADMIN_ID, BOUND_MERCHANT_ID);

        mockMvc.perform(get("/merchant-admin/products").cookie(adminSessionCookie(MERCHANT_ADMIN_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].merchant.id").value(BOUND_MERCHANT_ID));
    }

    @Test
    void merchantAdminCannotQueryUnboundMerchantProducts() throws Exception {
        seedMerchantAdminWithBinding(MERCHANT_ADMIN_ID, BOUND_MERCHANT_ID);

        mockMvc.perform(get("/merchant-admin/products")
                        .cookie(adminSessionCookie(MERCHANT_ADMIN_ID))
                        .param("merchantId", UNBOUND_MERCHANT_ID))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail.code").value("MERCHANT_SCOPE_FORBIDDEN"));
    }

    @Test
    void merchantAdminCanCreateUpdateAndDeactivateBoundProduct() throws Exception {
        seedMerchantAdminWithBinding(MERCHANT_ADMIN_ID, BOUND_MERCHANT_ID);

        String createdBody = mockMvc.perform(multipart("/merchant-admin/products")
                        .file(image("front.png"))
                        .cookie(adminSessionCookie(MERCHANT_ADMIN_ID))
                        .param("payload", validCreateProductRequest())
                        .param("newImageClientIds", "new-front")
                        .param("imageOrder", "new:new-front"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.merchant.id").value(BOUND_MERCHANT_ID))
                .andExpect(jsonPath("$.status").value("INACTIVE"))
                .andExpect(jsonPath("$.images[0].isPrimary").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String createdProductId = objectMapper.readTree(createdBody).path("id").asText();
        String retainedImageId = objectMapper.readTree(createdBody).path("images").get(0).path("id").asText();
        assertThat(createdProductId).isNotBlank();

        mockMvc.perform(multipart("/merchant-admin/products/{productId}", createdProductId)
                        .file(image("gallery.png"))
                        .cookie(adminSessionCookie(MERCHANT_ADMIN_ID))
                        .param("payload", """
                                {
                                  "merchantId": "%s",
                                  "productCode": "PRD-MERCHANT-NEW-001",
                                  "name": "Merchant Test Phone Pro",
                                  "description": "Updated merchant-managed smartphone listing",
                                  "categoryId": "f6f2c39a-1438-4e90-bcb2-bcb4db719001",
                                  "skus": [
                                    {
                                      "skuCode": "SKU-MERCHANT-NEW-001",
                                      "skuName": "Black / 128GB",
                                      "attributesJson": "{\\"color\\":\\"black\\",\\"storage\\":\\"128GB\\"}",
                                      "unitPriceAmount": 1350.00,
                                      "currencyCode": "GHS",
                                      "availableQuantity": 8
                                    }
                                  ]
                                }
                                """.formatted(BOUND_MERCHANT_ID))
                        .param("retainImageIds", retainedImageId)
                        .param("newImageClientIds", "new-gallery")
                        .param("imageOrder", "new:new-gallery", "existing:" + retainedImageId)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Merchant Test Phone Pro"))
                .andExpect(jsonPath("$.images[0].isPrimary").value(true));

        mockMvc.perform(post("/merchant-admin/products/{productId}/deactivate", createdProductId)
                        .cookie(adminSessionCookie(MERCHANT_ADMIN_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "merchantId": "%s"
                                }
                                """.formatted(BOUND_MERCHANT_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));

        Integer createdCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM products WHERE merchant_id = ? AND product_code = 'PRD-MERCHANT-NEW-001'",
                Integer.class,
                BOUND_MERCHANT_ID
        );
        assertThat(createdCount).isEqualTo(1);
    }

    @Test
    void merchantAdminCanUpdateBoundProductViaPostMultipartUpdate() throws Exception {
        seedMerchantAdminWithBinding(MERCHANT_ADMIN_ID, BOUND_MERCHANT_ID);

        String createdBody = mockMvc.perform(multipart("/merchant-admin/products")
                        .file(image("front.png"))
                        .cookie(adminSessionCookie(MERCHANT_ADMIN_ID))
                        .param("payload", validCreateProductRequest())
                        .param("newImageClientIds", "new-front")
                        .param("imageOrder", "new:new-front"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String createdProductId = objectMapper.readTree(createdBody).path("id").asText();
        String retainedImageId = objectMapper.readTree(createdBody).path("images").get(0).path("id").asText();

        mockMvc.perform(multipart("/merchant-admin/products/{productId}", createdProductId)
                        .file(image("gallery-post.png"))
                        .cookie(adminSessionCookie(MERCHANT_ADMIN_ID))
                        .param("payload", """
                                {
                                  "merchantId": "%s",
                                  "productCode": "PRD-MERCHANT-NEW-001",
                                  "name": "Merchant Test Phone Post Update",
                                  "description": "Updated via post multipart",
                                  "categoryId": "f6f2c39a-1438-4e90-bcb2-bcb4db719001",
                                  "skus": [
                                    {
                                      "skuCode": "SKU-MERCHANT-NEW-001",
                                      "skuName": "Black / 128GB",
                                      "attributesJson": "{\\"color\\":\\"black\\",\\"storage\\":\\"128GB\\"}",
                                      "unitPriceAmount": 1350.00,
                                      "currencyCode": "GHS",
                                      "availableQuantity": 8
                                    }
                                  ]
                                }
                                """.formatted(BOUND_MERCHANT_ID))
                        .param("retainImageIds", retainedImageId)
                        .param("newImageClientIds", "new-gallery-post")
                        .param("imageOrder", "new:new-gallery-post", "existing:" + retainedImageId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Merchant Test Phone Post Update"))
                .andExpect(jsonPath("$.images[0].isPrimary").value(true));
    }

    @Test
    void merchantAdminCanCreateProductWithAttributesAndSkus() throws Exception {
        seedMerchantAdminWithBinding(MERCHANT_ADMIN_ID, BOUND_MERCHANT_ID);

        mockMvc.perform(multipart("/merchant-admin/products")
                        .file(image("front.png"))
                        .file(image("back.png"))
                        .cookie(adminSessionCookie(MERCHANT_ADMIN_ID))
                        .param("payload", validSpuSkuCreateRequest())
                        .param("newImageClientIds", "front-client", "back-client")
                        .param("imageOrder", "new:front-client", "new:back-client"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.spu.name").value("Merchant Test Phone"))
                .andExpect(jsonPath("$.spu.brandId").value("brand-tecno-001"))
                .andExpect(jsonPath("$.images.length()").value(2))
                .andExpect(jsonPath("$.skus.length()").value(2))
                .andExpect(jsonPath("$.attributes.length()").value(3))
                .andExpect(jsonPath("$.skus[0].stock").value(20))
                .andExpect(jsonPath("$.skus[0].lockedStock").value(0));
    }

    @Test
    void merchantAdminCanCreateMultipleProductsWithoutProvidingProductCode() throws Exception {
        seedMerchantAdminWithBinding(MERCHANT_ADMIN_ID, BOUND_MERCHANT_ID);

        mockMvc.perform(multipart("/merchant-admin/products")
                        .file(image("front-1.png"))
                        .cookie(adminSessionCookie(MERCHANT_ADMIN_ID))
                        .param("payload", """
                                {
                                  "merchantId": "%s",
                                  "name": "Auto Code Phone",
                                  "description": "first product without explicit product code",
                                  "categoryId": "f6f2c39a-1438-4e90-bcb2-bcb4db719001",
                                  "skus": [
                                    {
                                      "skuCode": "SKU-AUTO-CODE-001",
                                      "skuName": "Black / 128GB",
                                      "attributesJson": "{\\"color\\":\\"black\\",\\"storage\\":\\"128GB\\"}",
                                      "unitPriceAmount": 1250.00,
                                      "currencyCode": "GHS",
                                      "availableQuantity": 12
                                    }
                                  ]
                                }
                                """.formatted(BOUND_MERCHANT_ID))
                        .param("newImageClientIds", "new-front-1")
                        .param("imageOrder", "new:new-front-1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.productCode").isNotEmpty());

        mockMvc.perform(multipart("/merchant-admin/products")
                        .file(image("front-2.png"))
                        .cookie(adminSessionCookie(MERCHANT_ADMIN_ID))
                        .param("payload", """
                                {
                                  "merchantId": "%s",
                                  "name": "Auto Code Phone",
                                  "description": "second product without explicit product code",
                                  "categoryId": "f6f2c39a-1438-4e90-bcb2-bcb4db719001",
                                  "skus": [
                                    {
                                      "skuCode": "SKU-AUTO-CODE-002",
                                      "skuName": "Blue / 256GB",
                                      "attributesJson": "{\\"color\\":\\"blue\\",\\"storage\\":\\"256GB\\"}",
                                      "unitPriceAmount": 1450.00,
                                      "currencyCode": "GHS",
                                      "availableQuantity": 8
                                    }
                                  ]
                                }
                                """.formatted(BOUND_MERCHANT_ID))
                        .param("newImageClientIds", "new-front-2")
                        .param("imageOrder", "new:new-front-2"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.productCode").isNotEmpty());
    }

    @Test
    void merchantAdminCanGetBoundProductDetailForEditor() throws Exception {
        seedMerchantAdminWithBinding(MERCHANT_ADMIN_ID, BOUND_MERCHANT_ID);

        mockMvc.perform(get("/merchant-admin/products/{productId}", "6db6eb8a-c00e-4c85-8bd8-6ae76fd4f8d5")
                        .cookie(adminSessionCookie(MERCHANT_ADMIN_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("6db6eb8a-c00e-4c85-8bd8-6ae76fd4f8d5"))
                .andExpect(jsonPath("$.spu.brandId").value("brand-tecno-001"))
                .andExpect(jsonPath("$.attributes.length()").value(2))
                .andExpect(jsonPath("$.skus[0].specsJson").isNotEmpty());
    }

    @Test
    void merchantAdminCannotRemoveAllProductImagesOnUpdate() throws Exception {
        seedMerchantAdminWithBinding(MERCHANT_ADMIN_ID, BOUND_MERCHANT_ID);

        String createdBody = mockMvc.perform(multipart("/merchant-admin/products")
                        .file(image("front.png"))
                        .cookie(adminSessionCookie(MERCHANT_ADMIN_ID))
                        .param("payload", validCreateProductRequest())
                        .param("newImageClientIds", "new-front")
                        .param("imageOrder", "new:new-front"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String createdProductId = objectMapper.readTree(createdBody).path("id").asText();

        mockMvc.perform(multipart("/merchant-admin/products/{productId}", createdProductId)
                        .cookie(adminSessionCookie(MERCHANT_ADMIN_ID))
                        .param("payload", validCreateProductRequest())
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail.code").value("PRODUCT_IMAGE_REQUIRED"));
    }

    @Test
    void merchantAdminCanUpdateSeededProductWhenSkuIsReferencedByCart() throws Exception {
        seedMerchantAdminWithBinding(MERCHANT_ADMIN_ID, BOUND_MERCHANT_ID);
        String existingImageId = seedProductImage(SEEDED_PRODUCT_ID, "/public/uploads/products/seeded-phone.png");

        mockMvc.perform(multipart("/merchant-admin/products/{productId}", SEEDED_PRODUCT_ID)
                        .cookie(adminSessionCookie(MERCHANT_ADMIN_ID))
                        .param("payload", """
                                {
                                  "merchantId": "%s",
                                  "productCode": "PRD-ZOKO-PHONE-X1",
                                  "name": "Zoko Phone X1 Updated",
                                  "description": "Updated seeded merchant product",
                                  "categoryId": "f6f2c39a-1438-4e90-bcb2-bcb4db719001",
                                  "brandId": "brand-tecno-001",
                                  "skus": [
                                    {
                                      "skuCode": "X1-BLACK-128",
                                      "skuName": "Black / 128GB",
                                      "specsJson": {
                                        "storage": "128GB",
                                        "color": "black"
                                      },
                                      "price": 1299.00,
                                      "originalPrice": 1399.00,
                                      "costPrice": 950.00,
                                      "stock": 14
                                    }
                                  ]
                                }
                                """.formatted(BOUND_MERCHANT_ID))
                        .param("retainImageIds", existingImageId)
                        .param("imageOrder", "existing:" + existingImageId)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Zoko Phone X1 Updated"))
                .andExpect(jsonPath("$.skus[0].id").value(SEEDED_SKU_ID))
                .andExpect(jsonPath("$.skus[0].priceAmount").value("1299.00"))
                .andExpect(jsonPath("$.skus[0].stock").value(14));

        String cartSkuId = jdbcTemplate.queryForObject(
                "SELECT sku_id FROM cart_items WHERE id = '1a8ffb74-6b67-4638-b73a-4b15c6bc866a'",
                String.class
        );
        assertThat(cartSkuId).isEqualTo(SEEDED_SKU_ID);
    }

    @Test
    void merchantAdminCanActivateOwnInactiveProduct() throws Exception {
        seedMerchantAdminWithBinding(MERCHANT_ADMIN_ID, BOUND_MERCHANT_ID);

        String createdBody = mockMvc.perform(multipart("/merchant-admin/products")
                        .file(image("front.png"))
                        .cookie(adminSessionCookie(MERCHANT_ADMIN_ID))
                        .param("payload", validCreateProductRequest())
                        .param("newImageClientIds", "new-front")
                        .param("imageOrder", "new:new-front"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("INACTIVE"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String createdProductId = objectMapper.readTree(createdBody).path("id").asText();

        mockMvc.perform(post("/merchant-admin/products/{productId}/activate", createdProductId)
                        .cookie(adminSessionCookie(MERCHANT_ADMIN_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "merchantId": "%s"
                                }
                                """.formatted(BOUND_MERCHANT_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(createdProductId))
                .andExpect(jsonPath("$.merchant.id").value(BOUND_MERCHANT_ID))
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void merchantAdminCannotActivateProductForUnboundMerchant() throws Exception {
        seedMerchantAdminWithBinding(MERCHANT_ADMIN_ID, BOUND_MERCHANT_ID);

        String createdBody = mockMvc.perform(multipart("/merchant-admin/products")
                        .file(image("front.png"))
                        .cookie(adminSessionCookie(MERCHANT_ADMIN_ID))
                        .param("payload", validCreateProductRequest())
                        .param("newImageClientIds", "new-front")
                        .param("imageOrder", "new:new-front"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String createdProductId = objectMapper.readTree(createdBody).path("id").asText();

        mockMvc.perform(post("/merchant-admin/products/{productId}/activate", createdProductId)
                        .cookie(adminSessionCookie(MERCHANT_ADMIN_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "merchantId": "%s"
                                }
                                """.formatted(UNBOUND_MERCHANT_ID)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail.code").value("MERCHANT_SCOPE_FORBIDDEN"));
    }

    @Test
    void merchantAdminCannotActivateProductOutsideInactiveStatus() throws Exception {
        seedMerchantAdminWithBinding(MERCHANT_ADMIN_ID, BOUND_MERCHANT_ID);

        mockMvc.perform(post("/merchant-admin/products/{productId}/activate", SEEDED_PRODUCT_ID)
                        .cookie(adminSessionCookie(MERCHANT_ADMIN_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "merchantId": "%s"
                                }
                                """.formatted(BOUND_MERCHANT_ID)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail.code").value("PRODUCT_ALREADY_IN_TARGET_STATE"));
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

    private String validCreateProductRequest() {
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
                """.formatted(BOUND_MERCHANT_ID);
    }

    private String validSpuSkuCreateRequest() {
        return """
                {
                  "merchantId": "%s",
                  "name": "Merchant Test Phone",
                  "categoryId": "f6f2c39a-1438-4e90-bcb2-bcb4db719001",
                  "brandId": "brand-tecno-001",
                  "descriptionHtml": "<p>Merchant smartphone for Ghana market</p>",
                  "attributes": [
                    {
                      "attributeCode": "screen_size",
                      "attributeId": "attr-screen-size-001",
                      "valueText": "6.7 inch"
                    },
                    {
                      "attributeCode": "network",
                      "attributeId": "attr-network-001",
                      "valueText": "5G"
                    },
                    {
                      "customAttributeName": "launch_series",
                      "type": "TEXT",
                      "valueText": "2026 flagship"
                    }
                  ],
                  "skus": [
                    {
                      "skuCode": "sku-phone-black-128",
                      "specsJson": {
                        "color": "black",
                        "storage": "128GB"
                      },
                      "price": 1250.00,
                      "originalPrice": 1350.00,
                      "costPrice": 900.00,
                      "stock": 20
                    },
                    {
                      "skuCode": "sku-phone-blue-256",
                      "specsJson": {
                        "color": "blue",
                        "storage": "256GB"
                      },
                      "price": 1450.00,
                      "originalPrice": 1550.00,
                      "costPrice": 1100.00,
                      "stock": 10
                    }
                  ]
                }
                """.formatted(BOUND_MERCHANT_ID);
    }

    private String seedProductImage(String productId, String imageUrl) {
        String imageId = UUID.randomUUID().toString();
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
                        VALUES (?, ?, ?, ?, 'image/png', 32, 'seed.png', 0, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        """,
                imageId,
                productId,
                "products/" + imageId + ".png",
                imageUrl
        );
        return imageId;
    }

    private MockMultipartFile image(String filename) {
        return new MockMultipartFile("newImages", filename, "image/png", new byte[] {1, 2, 3});
    }
}
