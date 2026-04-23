package com.zokomart.backend;

import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MerchantAdminOrderControllerTest {

    private static final String MERCHANT_ADMIN_ID = "admin-merchant-scope-order-001";
    private static final String BOUND_MERCHANT_ID = "c5ce3f6d-3ca0-4b44-84a0-d2bc3f520fa3";
    private static final String UNBOUND_MERCHANT_ID = "d7a6d1f6-7ab2-45d0-bf96-2f2f7c07f999";
    private static final String BUYER_ID = "00000000-0000-0000-0000-000000000101";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void merchantAdminOnlySeesBoundMerchantOrders() throws Exception {
        seedMerchantAdminWithBinding(MERCHANT_ADMIN_ID, BOUND_MERCHANT_ID);
        String orderId = createOrderAndReturnId();

        mockMvc.perform(get("/merchant-admin/orders").cookie(adminSessionCookie(MERCHANT_ADMIN_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(orderId))
                .andExpect(jsonPath("$.items[0].merchantId").value(BOUND_MERCHANT_ID));
    }

    @Test
    void merchantAdminCannotQueryUnboundMerchantOrders() throws Exception {
        seedMerchantAdminWithBinding(MERCHANT_ADMIN_ID, BOUND_MERCHANT_ID);

        mockMvc.perform(get("/merchant-admin/orders")
                        .cookie(adminSessionCookie(MERCHANT_ADMIN_ID))
                        .param("merchantId", UNBOUND_MERCHANT_ID))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail.code").value("MERCHANT_SCOPE_FORBIDDEN"));
    }

    @Test
    void merchantAdminCannotViewUnboundMerchantOrderDetail() throws Exception {
        seedMerchantAdminWithBinding(MERCHANT_ADMIN_ID, BOUND_MERCHANT_ID);
        String orderId = createOrderAndReturnId();

        seedUnboundMerchant();
        jdbcTemplate.update("UPDATE orders SET merchant_id = ? WHERE id = ?", UNBOUND_MERCHANT_ID, orderId);

        mockMvc.perform(get("/merchant-admin/orders/{orderId}", orderId)
                        .cookie(adminSessionCookie(MERCHANT_ADMIN_ID)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail.code").value("MERCHANT_SCOPE_FORBIDDEN"));
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

    private void seedUnboundMerchant() {
        jdbcTemplate.update(
                """
                        INSERT INTO merchants (
                            id,
                            merchant_code,
                            merchant_type,
                            display_name,
                            status,
                            created_at,
                            updated_at
                        )
                        VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        """,
                UNBOUND_MERCHANT_ID,
                "ZM-TP-UNBOUND-001",
                "THIRD_PARTY",
                "Unbound Merchant",
                "APPROVED"
        );
    }

    private Cookie adminSessionCookie(String adminUserId) {
        String token = StpUtil.createLoginSession(adminUserId);
        return new Cookie("satoken", token);
    }

    private String createOrderAndReturnId() throws Exception {
        String body = mockMvc.perform(
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
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode jsonNode = objectMapper.readTree(body);
        return jsonNode.get("id").asText();
    }
}
