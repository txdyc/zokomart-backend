package com.zokomart.backend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class OrderControllerTest {

    private static final String BUYER_ID = "00000000-0000-0000-0000-000000000101";
    private static final String EXISTING_CART_ID = "d7b9fb04-4f81-4ea5-918f-21a86ff72a8d";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createOrderGeneratesPendingPaymentOrderAndIntent() throws Exception {
        String responseBody = mockMvc.perform(
                        post("/orders")
                                .header("X-Buyer-Id", BUYER_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(validOrderRequest()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderNumber").value(org.hamcrest.Matchers.startsWith("ZKM-")))
                .andExpect(jsonPath("$.buyerId").value(BUYER_ID))
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"))
                .andExpect(jsonPath("$.merchantId").value("c5ce3f6d-3ca0-4b44-84a0-d2bc3f520fa3"))
                .andExpect(jsonPath("$.totalAmount").value("1250.00"))
                .andExpect(jsonPath("$.items[0].skuId").value("77cfce71-f8f5-44d5-9adc-0a76d5f65d5a"))
                .andExpect(jsonPath("$.paymentIntent.status").value("CREATED"))
                .andExpect(jsonPath("$.paymentIntent.amount").value("1250.00"))
                .andExpect(jsonPath("$.paymentIntent.expiresAt").isNotEmpty())
                .andExpect(jsonPath("$.shippingAddress.city").value("Accra"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode responseJson = objectMapper.readTree(responseBody);
        OffsetDateTime createdAt = OffsetDateTime.parse(responseJson.get("createdAt").asText());
        OffsetDateTime expiresAt = OffsetDateTime.parse(responseJson.get("paymentIntent").get("expiresAt").asText());
        assertThat(Duration.between(createdAt, expiresAt))
                .isBetween(Duration.ofMinutes(29), Duration.ofMinutes(31));

        mockMvc.perform(get("/cart").header("X-Buyer-Id", BUYER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.referenceTotalAmount").value("0.00"));
    }

    @Test
    void getOrderReturnsBuyerOwnedOrderDetail() throws Exception {
        String responseBody = mockMvc.perform(
                        post("/orders")
                                .header("X-Buyer-Id", BUYER_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(validOrderRequest()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode responseJson = objectMapper.readTree(responseBody);
        String orderId = responseJson.get("id").asText();

        mockMvc.perform(get("/orders/{orderId}", orderId).header("X-Buyer-Id", BUYER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId))
                .andExpect(jsonPath("$.buyerId").value(BUYER_ID))
                .andExpect(jsonPath("$.paymentIntent.status").value("CREATED"))
                .andExpect(jsonPath("$.paymentIntent.expiresAt").isNotEmpty())
                .andExpect(jsonPath("$.items[0].productName").value("Zoko Phone X1"));
    }

    @Test
    void createOrderRejectsEmptyCart() throws Exception {
        mockMvc.perform(
                        post("/orders")
                                .header("X-Buyer-Id", "00000000-0000-0000-0000-000000000999")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "shippingAddress": {
                                            "recipientName": "Kojo Mensah",
                                            "phoneNumber": "+233201234567",
                                            "addressLine1": "East Legon",
                                            "city": "Accra"
                                          }
                                        }
                                        """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail.code").value("EMPTY_CART"));
    }

    @Test
    void createOrderRejectsCrossMerchantCart() throws Exception {
        jdbcTemplate.update(
                """
                        INSERT INTO merchants (id, merchant_code, merchant_type, display_name, status, created_at, updated_at)
                        VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        """,
                "9a76fc72-7b48-4ac4-b53f-c8f7b90fb201",
                "ZM-TP-001",
                "THIRD_PARTY",
                "Accra Seller One",
                "ACTIVE"
        );
        jdbcTemplate.update(
                """
                        INSERT INTO products (id, merchant_id, product_code, name, description, status, is_self_operated, created_at, updated_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        """,
                "c9f8f76c-4820-43e8-8c1f-2f5e90af1201",
                "9a76fc72-7b48-4ac4-b53f-c8f7b90fb201",
                "PRD-TP-001",
                "Seller Phone Lite",
                "第三方商家测试商品",
                "ACTIVE",
                false
        );
        jdbcTemplate.update(
                """
                        INSERT INTO product_skus (id, product_id, sku_code, sku_name, attributes_json, unit_price_amount, currency_code, available_quantity, status, created_at, updated_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        """,
                "ad5c6c30-36ce-490d-b2f2-05ba3d326701",
                "c9f8f76c-4820-43e8-8c1f-2f5e90af1201",
                "SELLER-LITE-BLUE-64",
                "蓝色 / 64GB",
                "{\"color\":\"blue\",\"storage\":\"64GB\"}",
                800.00,
                "GHS",
                5,
                "ACTIVE"
        );
        jdbcTemplate.update(
                """
                        INSERT INTO cart_items (id, cart_id, merchant_id, product_id, sku_id, product_name_snapshot, sku_name_snapshot, reference_price_amount, currency_code, quantity, created_at, updated_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        """,
                "ad5c6c30-36ce-490d-b2f2-05ba3d326702",
                EXISTING_CART_ID,
                "9a76fc72-7b48-4ac4-b53f-c8f7b90fb201",
                "c9f8f76c-4820-43e8-8c1f-2f5e90af1201",
                "ad5c6c30-36ce-490d-b2f2-05ba3d326701",
                "Seller Phone Lite",
                "蓝色 / 64GB",
                800.00,
                "GHS",
                1
        );

        mockMvc.perform(
                        post("/orders")
                                .header("X-Buyer-Id", BUYER_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "shippingAddress": {
                                            "recipientName": "Kojo Mensah",
                                            "phoneNumber": "+233201234567",
                                            "addressLine1": "East Legon",
                                            "city": "Accra"
                                          }
                                        }
                                        """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail.code").value("MULTI_MERCHANT_CART"));
    }

    @Test
    void createOrderRejectsMissingShippingAddress() throws Exception {
        mockMvc.perform(
                        post("/orders")
                                .header("X-Buyer-Id", BUYER_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail.code").value("INVALID_REQUEST_BODY"))
                .andExpect(jsonPath("$.detail.meta.fields[0].field").value("shippingAddress"));
    }

    @Test
    void createOrderRejectsBlankShippingCity() throws Exception {
        mockMvc.perform(
                        post("/orders")
                                .header("X-Buyer-Id", BUYER_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "shippingAddress": {
                                            "recipientName": "Kojo Mensah",
                                            "phoneNumber": "+233201234567",
                                            "addressLine1": "East Legon",
                                            "city": "   "
                                          }
                                        }
                                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail.code").value("INVALID_REQUEST_BODY"))
                .andExpect(jsonPath("$.detail.meta.fields[0].field").value("shippingAddress.city"));
    }

    @Test
    void orderCreationLocksInventoryInsteadOfDeductingImmediately() throws Exception {
        mockMvc.perform(post("/orders")
                        .header("X-Buyer-Id", BUYER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validOrderRequest()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"));

        Integer stock = jdbcTemplate.queryForObject(
                "SELECT stock FROM product_skus WHERE id = ?",
                Integer.class,
                "77cfce71-f8f5-44d5-9adc-0a76d5f65d5a"
        );
        Integer lockedStock = jdbcTemplate.queryForObject(
                "SELECT locked_stock FROM product_skus WHERE id = ?",
                Integer.class,
                "77cfce71-f8f5-44d5-9adc-0a76d5f65d5a"
        );

        assertThat(stock).isEqualTo(15);
        assertThat(lockedStock).isEqualTo(1);
    }

    private String validOrderRequest() {
        return """
                {
                  "shippingAddress": {
                    "recipientName": "Kojo Mensah",
                    "phoneNumber": "+233201234567",
                    "addressLine1": "East Legon",
                    "addressLine2": "Block B",
                    "city": "Accra",
                    "region": "Greater Accra",
                    "countryCode": "GH"
                  }
                }
                """;
    }
}
