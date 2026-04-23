package com.zokomart.backend;

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

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CatalogCartControllerTest {

    private static final String BUYER_ID = "00000000-0000-0000-0000-000000000101";
    private static final String PRODUCT_ID = "6db6eb8a-c00e-4c85-8bd8-6ae76fd4f8d5";
    private static final String SKU_ID = "77cfce71-f8f5-44d5-9adc-0a76d5f65d5a";
    private static final String CART_ITEM_ID = "1a8ffb74-6b67-4638-b73a-4b15c6bc866a";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void listProductsReturnsSeededCatalog() throws Exception {
        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(PRODUCT_ID))
                .andExpect(jsonPath("$.items[0].merchantType").value("SELF_OPERATED"))
                .andExpect(jsonPath("$.items[0].priceAmount").value("1250.00"))
                .andExpect(jsonPath("$.items[0].thumbnailUrl").value("/public/uploads/products/zoko-x1-front.png"))
                .andExpect(jsonPath("$.total").value(1));
    }

    @Test
    void getProductReturnsSkuDetails() throws Exception {
        mockMvc.perform(get("/products/{productId}", PRODUCT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(PRODUCT_ID))
                .andExpect(jsonPath("$.primaryImageUrl").value("/public/uploads/products/zoko-x1-front.png"))
                .andExpect(jsonPath("$.defaultSkuId").value(SKU_ID))
                .andExpect(jsonPath("$.priceRange.minPriceAmount").value("1250.00"))
                .andExpect(jsonPath("$.priceRange.maxPriceAmount").value("1650.00"))
                .andExpect(jsonPath("$.priceRange.currencyCode").value("GHS"))
                .andExpect(jsonPath("$.optionGroups[*].code").value(hasItem("storage")))
                .andExpect(jsonPath("$.optionGroups[*].code").value(hasItem("color")))
                .andExpect(jsonPath("$.skus[0].id").value(SKU_ID))
                .andExpect(jsonPath("$.skus[0].availableQuantity").value(15))
                .andExpect(jsonPath("$.skus[0].originalPriceAmount").value("1350.00"))
                .andExpect(jsonPath("$.skus[0].optionValues[?(@.optionCode=='storage')].optionValue").value(hasItem("128GB")))
                .andExpect(jsonPath("$.skus[0].optionValues[?(@.optionCode=='color')].optionValue").value(hasItem("black")));
    }

    @Test
    void getProductReturnsEmptyOptionsWhenSpecsJsonInvalid() throws Exception {
        String productId = "01111111-2222-3333-4444-555555555555";
        String skuId = UUID.randomUUID().toString();

        jdbcTemplate.update(
                """
                        INSERT INTO products (
                            id,
                            merchant_id,
                            product_code,
                            name,
                            description,
                            description_html,
                            status,
                            is_self_operated,
                            category_id,
                            brand_id,
                            attributes_json,
                            created_at,
                            updated_at
                        )
                        VALUES (?, ?, ?, ?, ?, ?, 'APPROVED', TRUE, ?, ?, '{}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        """,
                productId,
                "c5ce3f6d-3ca0-4b44-84a0-d2bc3f520fa3",
                "PRD-BROKEN-SPECS",
                "Broken Specs Phone",
                "Broken specs json test product",
                "<p>Broken specs json test product</p>",
                "f6f2c39a-1438-4e90-bcb2-bcb4db719001",
                "brand-tecno-001"
        );

        jdbcTemplate.update(
                """
                        INSERT INTO product_skus (
                            id,
                            product_id,
                            spu_id,
                            sku_code,
                            sku_name,
                            attributes_json,
                            specs_json,
                            unit_price_amount,
                            price,
                            original_price,
                            cost_price,
                            currency_code,
                            available_quantity,
                            stock,
                            locked_stock,
                            status,
                            created_at,
                            updated_at
                        )
                        VALUES (?, ?, ?, ?, ?, '{}', '{broken', 999.00, 999.00, 1200.00, 700.00, 'GHS', 3, 3, 0, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        """,
                skuId,
                productId,
                productId,
                "BROKEN-SPECS-001",
                "Broken Specs Variant"
        );

        mockMvc.perform(get("/products/{productId}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(productId))
                .andExpect(jsonPath("$.optionGroups").isEmpty())
                .andExpect(jsonPath("$.skus[0].id").value(skuId))
                .andExpect(jsonPath("$.skus[0].optionValues").isEmpty());
    }

    @Test
    void getCartReturnsExistingCartForBuyer() throws Exception {
        mockMvc.perform(get("/cart").header("X-Buyer-Id", BUYER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.buyerId").value(BUYER_ID))
                .andExpect(jsonPath("$.items[0].id").value(CART_ITEM_ID))
                .andExpect(jsonPath("$.referenceTotalAmount").value("1250.00"));
    }

    @Test
    void getCartRequiresBuyerHeader() throws Exception {
        mockMvc.perform(get("/cart"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail.code").value("MISSING_BUYER_ID"))
                .andExpect(jsonPath("$.detail.meta.header").value("X-Buyer-Id"));
    }

    @Test
    void getCartRejectsBlankBuyerHeader() throws Exception {
        mockMvc.perform(get("/cart").header("X-Buyer-Id", "   "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail.code").value("MISSING_BUYER_ID"))
                .andExpect(jsonPath("$.detail.meta.header").value("X-Buyer-Id"));
    }

    @Test
    void addCartItemAccumulatesQuantityForSameSku() throws Exception {
        mockMvc.perform(
                        post("/cart/items")
                                .header("X-Buyer-Id", BUYER_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "skuId": "77cfce71-f8f5-44d5-9adc-0a76d5f65d5a",
                                          "quantity": 2
                                        }
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].quantity").value(3))
                .andExpect(jsonPath("$.referenceTotalAmount").value("3750.00"));
    }

    @Test
    void addCartItemRejectsNonPositiveQuantity() throws Exception {
        mockMvc.perform(
                        post("/cart/items")
                                .header("X-Buyer-Id", BUYER_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "skuId": "77cfce71-f8f5-44d5-9adc-0a76d5f65d5a",
                                          "quantity": 0
                                        }
                                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail.code").value("INVALID_REQUEST_BODY"))
                .andExpect(jsonPath("$.detail.meta.fields[0].field").value("quantity"));
    }

    @Test
    void addCartItemRejectsInvalidQuantityType() throws Exception {
        mockMvc.perform(
                        post("/cart/items")
                                .header("X-Buyer-Id", BUYER_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "skuId": "77cfce71-f8f5-44d5-9adc-0a76d5f65d5a",
                                          "quantity": "abc"
                                        }
                                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail.code").value("INVALID_REQUEST"));
    }

    @Test
    void updateCartItemChangesQuantity() throws Exception {
        mockMvc.perform(
                        patch("/cart/items/{itemId}", CART_ITEM_ID)
                                .header("X-Buyer-Id", BUYER_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "quantity": 4
                                        }
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].quantity").value(4))
                .andExpect(jsonPath("$.referenceTotalAmount").value("5000.00"));
    }

    @Test
    void updateCartItemRejectsNonPositiveQuantity() throws Exception {
        mockMvc.perform(
                        patch("/cart/items/{itemId}", CART_ITEM_ID)
                                .header("X-Buyer-Id", BUYER_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "quantity": 0
                                        }
                                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail.code").value("INVALID_REQUEST_BODY"))
                .andExpect(jsonPath("$.detail.meta.fields[0].field").value("quantity"));
    }
}
