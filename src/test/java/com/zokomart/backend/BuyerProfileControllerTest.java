package com.zokomart.backend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class BuyerProfileControllerTest {

    private static final String BUYER_ID = "00000000-0000-0000-0000-000000000101";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getMeReturnsBuyerAccountOverview() throws Exception {
        mockMvc.perform(get("/me").header("X-Buyer-Id", BUYER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile.buyerId").value(BUYER_ID))
                .andExpect(jsonPath("$.profile.fullName").value("Abena Mensah"))
                .andExpect(jsonPath("$.profile.phoneNumber").value("+233 24 567 8901"))
                .andExpect(jsonPath("$.profile.buyerRating").value("4.9"))
                .andExpect(jsonPath("$.profile.isVerified").value(true))
                .andExpect(jsonPath("$.stats.orders").value(12))
                .andExpect(jsonPath("$.stats.wishlist").value(8))
                .andExpect(jsonPath("$.stats.reviews").value(5))
                .andExpect(jsonPath("$.wallet.providerLabel").value("MTN MOMO WALLET"))
                .andExpect(jsonPath("$.wallet.maskedBalanceLabel").value("GH₵ ••••••"))
                .andExpect(jsonPath("$.wallet.isBalanceHidden").value(true))
                .andExpect(jsonPath("$.recentTransactions.length()").value(3))
                .andExpect(jsonPath("$.recentTransactions[0].title").value("ZokoMart Order #7821"))
                .andExpect(jsonPath("$.recentTransactions[0].amountLabel").value("-GH₵ 425"))
                .andExpect(jsonPath("$.recentOrders.length()").value(3))
                .andExpect(jsonPath("$.recentOrders[0].orderNumber").value("Order #SK-7821"))
                .andExpect(jsonPath("$.recentOrders[0].status").value("SHIPPED"))
                .andExpect(jsonPath("$.menuHints.activeOrdersLabel").value("3 active"))
                .andExpect(jsonPath("$.menuHints.wishlistLabel").value("8 items"))
                .andExpect(jsonPath("$.menuHints.savedAddressesLabel").value("2 saved"))
                .andExpect(jsonPath("$.menuHints.activeVouchersLabel").value("1 active"));
    }

    @Test
    void getMeReturnsNotFoundWhenBuyerProfileMissing() throws Exception {
        mockMvc.perform(get("/me").header("X-Buyer-Id", "00000000-0000-0000-0000-000000000999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail.code").value("BUYER_PROFILE_NOT_FOUND"));
    }
}
