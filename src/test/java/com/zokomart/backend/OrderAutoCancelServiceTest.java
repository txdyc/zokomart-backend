package com.zokomart.backend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zokomart.backend.order.service.OrderAutoCancelService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class OrderAutoCancelServiceTest {

    private static final String BUYER_ID = "00000000-0000-0000-0000-000000000101";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private OrderAutoCancelService orderAutoCancelService;

    @Test
    void autoCancelExpiredPendingPaymentOrder() throws Exception {
        String orderId = createOrderAndReturnId();
        String paymentIntentId = findPaymentIntentId(orderId);
        LocalDateTime timeoutMoment = LocalDateTime.now().minusMinutes(31);
        jdbcTemplate.update(
                "UPDATE payment_intents SET expires_at = ?, updated_at = ? WHERE id = ?",
                Timestamp.valueOf(timeoutMoment),
                Timestamp.valueOf(timeoutMoment),
                paymentIntentId
        );

        int cancelledCount = orderAutoCancelService.autoCancelExpiredOrders();

        assertThat(cancelledCount).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM orders WHERE id = ?", String.class, orderId))
                .isEqualTo("CANCELLED");
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM payment_intents WHERE id = ?", String.class, paymentIntentId))
                .isEqualTo("EXPIRED");
        assertThat(jdbcTemplate.queryForObject("SELECT cancelled_at IS NOT NULL FROM orders WHERE id = ?", Boolean.class, orderId))
                .isTrue();
        assertThat(jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM order_status_history
                        WHERE order_id = ?
                          AND from_status = 'PENDING_PAYMENT'
                          AND to_status = 'CANCELLED'
                          AND changed_by_actor_type = 'SYSTEM'
                          AND reason_code = 'PAYMENT_TIMEOUT_CANCELLED'
                        """,
                Integer.class,
                orderId
        )).isEqualTo(1);
    }

    @Test
    void doesNotCancelOrderWhenPaymentIntentIsStillValid() throws Exception {
        String orderId = createOrderAndReturnId();
        String paymentIntentId = findPaymentIntentId(orderId);
        LocalDateTime futureMoment = LocalDateTime.now().plusMinutes(5);
        jdbcTemplate.update(
                "UPDATE payment_intents SET expires_at = ?, updated_at = ? WHERE id = ?",
                Timestamp.valueOf(futureMoment),
                Timestamp.valueOf(LocalDateTime.now()),
                paymentIntentId
        );

        int cancelledCount = orderAutoCancelService.autoCancelExpiredOrders();

        assertThat(cancelledCount).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM orders WHERE id = ?", String.class, orderId))
                .isEqualTo("PENDING_PAYMENT");
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM payment_intents WHERE id = ?", String.class, paymentIntentId))
                .isEqualTo("CREATED");
    }

    @Test
    void secondScanSkipsAlreadyCancelledOrder() throws Exception {
        String orderId = createOrderAndReturnId();
        String paymentIntentId = findPaymentIntentId(orderId);
        LocalDateTime timeoutMoment = LocalDateTime.now().minusMinutes(31);
        jdbcTemplate.update(
                "UPDATE payment_intents SET expires_at = ?, updated_at = ? WHERE id = ?",
                Timestamp.valueOf(timeoutMoment),
                Timestamp.valueOf(timeoutMoment),
                paymentIntentId
        );

        int firstRun = orderAutoCancelService.autoCancelExpiredOrders();
        int secondRun = orderAutoCancelService.autoCancelExpiredOrders();

        assertThat(firstRun).isEqualTo(1);
        assertThat(secondRun).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM order_status_history WHERE order_id = ? AND reason_code = 'PAYMENT_TIMEOUT_CANCELLED'",
                Integer.class,
                orderId
        )).isEqualTo(1);
    }

    @Test
    void autoCancelReleasesInventoryLock() throws Exception {
        String orderId = createOrderAndReturnId();
        String paymentIntentId = findPaymentIntentId(orderId);
        LocalDateTime timeoutMoment = LocalDateTime.now().minusMinutes(31);
        jdbcTemplate.update(
                "UPDATE payment_intents SET expires_at = ?, updated_at = ? WHERE id = ?",
                Timestamp.valueOf(timeoutMoment),
                Timestamp.valueOf(timeoutMoment),
                paymentIntentId
        );

        int cancelledCount = orderAutoCancelService.autoCancelExpiredOrders();

        assertThat(cancelledCount).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT locked_stock FROM product_skus WHERE id = ?",
                Integer.class,
                "77cfce71-f8f5-44d5-9adc-0a76d5f65d5a"
        )).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM inventory_lock_records WHERE order_id = ?",
                String.class,
                orderId
        )).isEqualTo("RELEASED");
    }

    private String createOrderAndReturnId() throws Exception {
        String responseBody = mockMvc.perform(
                        post("/orders")
                                .header("X-Buyer-Id", BUYER_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(validOrderRequest()))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode responseJson = objectMapper.readTree(responseBody);
        return responseJson.get("id").asText();
    }

    private String findPaymentIntentId(String orderId) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM payment_intents WHERE order_id = ?",
                String.class,
                orderId
        );
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
