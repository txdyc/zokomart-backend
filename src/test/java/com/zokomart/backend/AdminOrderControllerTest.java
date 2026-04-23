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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminOrderControllerTest {

    private static final String PLATFORM_ADMIN_ID = "admin-platform-readonly-order-001";
    private static final String BUYER_ID = "00000000-0000-0000-0000-000000000101";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void listAdminOrdersRequiresAuthenticatedPlatformAdmin() throws Exception {
        mockMvc.perform(get("/admin/orders"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail.code").value("ADMIN_UNAUTHORIZED"));
    }

    @Test
    void listAdminOrdersReturnsPaymentIntentExpiry() throws Exception {
        String orderId = createOrderAndReturnId();

        mockMvc.perform(get("/admin/orders").cookie(loginAsPlatformAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(orderId))
                .andExpect(jsonPath("$.items[0].paymentIntent.expiresAt").isNotEmpty());
    }

    @Test
    void listAdminOrdersSupportsDateRangeFilters() throws Exception {
        createOrderAndReturnId();

        mockMvc.perform(get("/admin/orders")
                        .cookie(loginAsPlatformAdmin())
                        .param("from", "2099-01-01")
                        .param("to", "2099-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void cancelOrderIsForbiddenForPlatformAdmin() throws Exception {
        String orderId = createOrderAndReturnId();

        mockMvc.perform(
                        post("/admin/orders/{id}/cancel", orderId)
                                .cookie(loginAsPlatformAdmin())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "reason": "Buyer unreachable after review"
                                        }
                                        """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail.code").value("ADMIN_FORBIDDEN"));
    }

    private Cookie loginAsPlatformAdmin() {
        seedAdminUser(PLATFORM_ADMIN_ID, "platform.review.order", "Platform Order Reader", "PLATFORM_ADMIN");
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
