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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminMerchantControllerTest {

    private static final String PLATFORM_ADMIN_ID = "admin-platform-readonly-merchant-001";
    private static final String MERCHANT_ID = "c5ce3f6d-3ca0-4b44-84a0-d2bc3f520fa3";
    private static final String BUYER_ID = "00000000-0000-0000-0000-000000000101";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void listAdminMerchantsRequiresAuthenticatedPlatformAdmin() throws Exception {
        mockMvc.perform(get("/admin/merchants"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail.code").value("ADMIN_UNAUTHORIZED"));
    }

    @Test
    void listAdminMerchantsReturnsSeededMerchantForPlatformAdmin() throws Exception {
        mockMvc.perform(get("/admin/merchants").cookie(loginAsPlatformAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(MERCHANT_ID))
                .andExpect(jsonPath("$.items[0].status").value("APPROVED"))
                .andExpect(jsonPath("$.items[0].productCount").value(1));
    }

    @Test
    void getMerchantDetailReturnsBusinessSummaryAndOrderBreakdown() throws Exception {
        createOrderAndReturnId();

        mockMvc.perform(get("/admin/merchants/{id}", MERCHANT_ID).cookie(loginAsPlatformAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.merchant.id").value(MERCHANT_ID))
                .andExpect(jsonPath("$.merchant.merchantCode").value("ZM-SELF-001"))
                .andExpect(jsonPath("$.summary.orders7d").value(1))
                .andExpect(jsonPath("$.summary.orders30d").value(1))
                .andExpect(jsonPath("$.summary.gmv30d").value("1250.00"))
                .andExpect(jsonPath("$.summary.pendingPaymentOrders").value(1))
                .andExpect(jsonPath("$.orderStatusBreakdown.PENDING_PAYMENT").value(1))
                .andExpect(jsonPath("$.orderStatusBreakdown.CANCELLED").value(0));
    }

    @Test
    void createMerchantIsForbiddenForPlatformAdmin() throws Exception {
        mockMvc.perform(
                        post("/admin/merchants")
                                .cookie(loginAsPlatformAdmin())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "merchantCode": "ZM-TP-NEW-001",
                                          "displayName": "Tema Fresh Market",
                                          "merchantType": "THIRD_PARTY"
                                        }
                                        """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail.code").value("ADMIN_FORBIDDEN"));
    }

    @Test
    void listMerchantOrdersSupportsStatusAndTimeFilters() throws Exception {
        String orderId = createOrderAndReturnId();

        mockMvc.perform(get("/admin/merchants/{id}/orders", MERCHANT_ID)
                        .cookie(loginAsPlatformAdmin())
                        .param("status", "PENDING_PAYMENT")
                        .param("from", "2020-01-01")
                        .param("to", "2099-12-31")
                        .param("page", "1")
                        .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(orderId))
                .andExpect(jsonPath("$.items[0].merchantId").value(MERCHANT_ID))
                .andExpect(jsonPath("$.items[0].status").value("PENDING_PAYMENT"))
                .andExpect(jsonPath("$.items[0].paymentIntent.expiresAt").isNotEmpty())
                .andExpect(jsonPath("$.total").value(1));
    }

    @Test
    void exportMerchantOrdersReturnsCsvForCurrentFilters() throws Exception {
        String orderId = createOrderAndReturnId();

        mockMvc.perform(get("/admin/merchants/{id}/orders/export", MERCHANT_ID)
                        .cookie(loginAsPlatformAdmin())
                        .param("status", "PENDING_PAYMENT"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", org.hamcrest.Matchers.containsString("text/csv")))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("merchant-ZM-SELF-001-orders-")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("merchantCode,merchantName,orderId,orderNumber")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(orderId)))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("ZM-SELF-001")));
    }

    @Test
    void suspendMerchantIsForbiddenForPlatformAdmin() throws Exception {
        mockMvc.perform(
                        post("/admin/merchants/{id}/suspend", MERCHANT_ID)
                                .cookie(loginAsPlatformAdmin())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "reason": "Compliance review"
                                        }
                                        """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail.code").value("ADMIN_FORBIDDEN"));
    }

    private Cookie loginAsPlatformAdmin() {
        seedAdminUser(PLATFORM_ADMIN_ID, "platform.review.merchant", "Platform Merchant Reader", "PLATFORM_ADMIN");
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
