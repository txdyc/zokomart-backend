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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MerchantOrderControllerTest {

    private static final String BUYER_ID = "00000000-0000-0000-0000-000000000101";
    private static final String MERCHANT_ID = "c5ce3f6d-3ca0-4b44-84a0-d2bc3f520fa3";
    private static final String OTHER_MERCHANT_ID = "9a76fc72-7b48-4ac4-b53f-c8f7b90fb201";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void merchantCanOnlySeeOwnOrders() throws Exception {
        createOrderAndReturnId();
        insertOtherMerchantOrder();

        mockMvc.perform(get("/merchant/orders").header("X-Merchant-Id", MERCHANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].merchantId").value(MERCHANT_ID))
                .andExpect(jsonPath("$.items[0].status").value("PENDING_PAYMENT"))
                .andExpect(jsonPath("$.items[0].buyerDisplayName").value("Kojo Mensah"));
    }

    @Test
    void listMerchantOrdersRequiresMerchantHeader() throws Exception {
        mockMvc.perform(get("/merchant/orders"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail.code").value("MISSING_MERCHANT_ID"))
                .andExpect(jsonPath("$.detail.meta.header").value("X-Merchant-Id"));
    }

    @Test
    void listMerchantOrdersRejectsBlankMerchantHeader() throws Exception {
        mockMvc.perform(get("/merchant/orders").header("X-Merchant-Id", "   "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail.code").value("MISSING_MERCHANT_ID"))
                .andExpect(jsonPath("$.detail.meta.header").value("X-Merchant-Id"));
    }

    @Test
    void merchantOrderDetailReturnsItemsAndFulfillmentStatus() throws Exception {
        String orderId = createOrderAndReturnId();

        mockMvc.perform(get("/merchant/orders/{orderId}", orderId).header("X-Merchant-Id", MERCHANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId))
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"))
                .andExpect(jsonPath("$.items[0].productName").value("Zoko Phone X1"))
                .andExpect(jsonPath("$.shippingAddress.city").value("Accra"))
                .andExpect(jsonPath("$.fulfillment.status").value("PENDING"))
                .andExpect(jsonPath("$.fulfillment.events.length()").value(0));
    }

    @Test
    void merchantOrderDetailReturnsFulfillmentEventsInTimelineOrder() throws Exception {
        String orderId = createOrderAndReturnId();

        mockMvc.perform(
                        post("/merchant/orders/{orderId}/fulfillment-events", orderId)
                                .header("X-Merchant-Id", MERCHANT_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "status": "PREPARING",
                                          "notes": "已开始备货"
                                        }
                                        """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/merchant/orders/{orderId}", orderId).header("X-Merchant-Id", MERCHANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fulfillment.status").value("PREPARING"))
                .andExpect(jsonPath("$.fulfillment.events.length()").value(1))
                .andExpect(jsonPath("$.fulfillment.events[0].status").value("PREPARING"))
                .andExpect(jsonPath("$.fulfillment.events[0].notes").value("已开始备货"))
                .andExpect(jsonPath("$.fulfillment.events[0].createdAt").isNotEmpty());
    }

    @Test
    void merchantCannotReadAnotherMerchantsOrder() throws Exception {
        String orderId = createOrderAndReturnId();

        mockMvc.perform(get("/merchant/orders/{orderId}", orderId).header("X-Merchant-Id", OTHER_MERCHANT_ID))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail.code").value("FORBIDDEN_MERCHANT_ORDER_ACCESS"));
    }

    @Test
    void createFulfillmentEventMovesOrderToPreparing() throws Exception {
        String orderId = createOrderAndReturnId();

        mockMvc.perform(
                        post("/merchant/orders/{orderId}/fulfillment-events", orderId)
                                .header("X-Merchant-Id", MERCHANT_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "status": "PREPARING",
                                          "notes": "已开始备货"
                                        }
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(orderId))
                .andExpect(jsonPath("$.fulfillment.status").value("PREPARING"))
                .andExpect(jsonPath("$.event.status").value("PREPARING"))
                .andExpect(jsonPath("$.event.notes").value("已开始备货"));
    }

    @Test
    void createFulfillmentEventFailsWhenFulfillmentRecordIsMissing() throws Exception {
        String orderId = createOrderAndReturnId();
        jdbcTemplate.update("DELETE FROM fulfillment_records WHERE order_id = ?", orderId);

        mockMvc.perform(
                        post("/merchant/orders/{orderId}/fulfillment-events", orderId)
                                .header("X-Merchant-Id", MERCHANT_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "status": "PREPARING",
                                          "notes": "record missing"
                                        }
                                        """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail.code").value("FULFILLMENT_RECORD_NOT_FOUND"));
    }

    @Test
    void createFulfillmentEventRejectsUnknownStatus() throws Exception {
        String orderId = createOrderAndReturnId();

        mockMvc.perform(
                        post("/merchant/orders/{orderId}/fulfillment-events", orderId)
                                .header("X-Merchant-Id", MERCHANT_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "status": "PACKED",
                                          "notes": "unknown status"
                                        }
                                        """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail.code").value("INVALID_FULFILLMENT_STATUS"));
    }

    @Test
    void createFulfillmentEventRejectsIllegalTransition() throws Exception {
        String orderId = createOrderAndReturnId();

        mockMvc.perform(
                        post("/merchant/orders/{orderId}/fulfillment-events", orderId)
                                .header("X-Merchant-Id", MERCHANT_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "status": "PREPARING",
                                          "notes": "start preparing"
                                        }
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fulfillment.status").value("PREPARING"));

        mockMvc.perform(
                        post("/merchant/orders/{orderId}/fulfillment-events", orderId)
                                .header("X-Merchant-Id", MERCHANT_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "status": "PENDING",
                                          "notes": "move backward"
                                        }
                                        """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail.code").value("INVALID_FULFILLMENT_TRANSITION"));
    }

    @Test
    void createFulfillmentEventRejectsBlankStatusBeforeBusinessLogic() throws Exception {
        String orderId = createOrderAndReturnId();

        mockMvc.perform(
                        post("/merchant/orders/{orderId}/fulfillment-events", orderId)
                                .header("X-Merchant-Id", MERCHANT_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "status": "   ",
                                          "notes": "blank status"
                                        }
                                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail.code").value("INVALID_REQUEST_BODY"))
                .andExpect(jsonPath("$.detail.meta.fields[0].field").value("status"));
    }

    @Test
    void cannotShipPendingPaymentOrderBeforePayment() throws Exception {
        String orderId = createOrderAndReturnId();

        mockMvc.perform(
                        post("/merchant/orders/{orderId}/fulfillment-events", orderId)
                                .header("X-Merchant-Id", MERCHANT_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "status": "SHIPPED",
                                          "notes": "提前发货"
                                        }
                                        """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail.code").value("ORDER_NOT_READY_FOR_FULFILLMENT"));
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
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode json = objectMapper.readTree(body);
        return json.get("id").asText();
    }

    private void insertOtherMerchantOrder() {
        jdbcTemplate.update(
                """
                        INSERT INTO merchants (id, merchant_code, merchant_type, display_name, status, created_at, updated_at)
                        VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        """,
                OTHER_MERCHANT_ID,
                "ZM-TP-001",
                "THIRD_PARTY",
                "Accra Seller One",
                "ACTIVE"
        );
        jdbcTemplate.update(
                """
                        INSERT INTO orders (
                            id, order_number, buyer_id, merchant_id, status, currency_code,
                            subtotal_amount, shipping_amount, discount_amount, total_amount,
                            recipient_name_snapshot, phone_number_snapshot,
                            address_line1_snapshot, address_line2_snapshot, city_snapshot, region_snapshot, country_code_snapshot,
                            placed_at, created_at, updated_at
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        """,
                "b2e2f14d-0e86-4e55-82c0-1c3a4c5d6701",
                "ZKM-20260414-OTHER01",
                "00000000-0000-0000-0000-000000000202",
                OTHER_MERCHANT_ID,
                "PENDING_PAYMENT",
                "GHS",
                800.00,
                0.00,
                0.00,
                800.00,
                "Ama Boateng",
                "+233201111111",
                "Tema",
                null,
                "Accra",
                "Greater Accra",
                "GH"
        );
        jdbcTemplate.update(
                """
                        INSERT INTO fulfillment_records (id, order_id, merchant_id, status, created_at, updated_at)
                        VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        """,
                "b2e2f14d-0e86-4e55-82c0-1c3a4c5d6702",
                "b2e2f14d-0e86-4e55-82c0-1c3a4c5d6701",
                OTHER_MERCHANT_ID,
                "PENDING"
        );
    }
}
