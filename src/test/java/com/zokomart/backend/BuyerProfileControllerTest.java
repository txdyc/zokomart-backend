package com.zokomart.backend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
        mockMvc.perform(get("/me").header("Authorization", "Bearer " + loginAsBuyer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile.buyerId").value(BUYER_ID))
                .andExpect(jsonPath("$.profile.fullName").value("Abena Mensah"))
                .andExpect(jsonPath("$.profile.phoneNumber").value("+233 24 567 8901"))
                .andExpect(jsonPath("$.profile.bio").value("Loves discovering handmade finds across Ghana."))
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
    void getMeRequiresBuyerAuthentication() throws Exception {
        mockMvc.perform(get("/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail.code").value("BUYER_UNAUTHORIZED"));
    }

    @Test
    void patchMeProfileUpdatesBuyerProfile() throws Exception {
        mockMvc.perform(patch("/me/profile")
                        .header("Authorization", "Bearer " + loginAsBuyer())
                        .contentType("application/json")
                        .content("""
                                {
                                  "nickname": "Abena Market Scout",
                                  "bio": "Finds handmade gems across Accra.",
                                  "avatarUrl": "/public/uploads/buyers/2026/04/new-avatar.png"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile.fullName").value("Abena Market Scout"))
                .andExpect(jsonPath("$.profile.bio").value("Finds handmade gems across Accra."))
                .andExpect(jsonPath("$.profile.avatarUrl").value("/public/uploads/buyers/2026/04/new-avatar.png"));
    }

    @Test
    void uploadBuyerAvatarReturnsPublicUrl() throws Exception {
        MockMultipartFile avatar = new MockMultipartFile(
                "file",
                "avatar.png",
                "image/png",
                new byte[] {1, 2, 3}
        );

        mockMvc.perform(multipart("/me/avatar")
                        .file(avatar)
                        .header("Authorization", "Bearer " + loginAsBuyer()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.avatarUrl").value(org.hamcrest.Matchers.containsString("/public/uploads/buyers/")))
                .andExpect(jsonPath("$.contentType").value("image/png"))
                .andExpect(jsonPath("$.sizeBytes").value(3));
    }

    @Test
    void uploadBuyerAvatarRequiresBuyerAuthentication() throws Exception {
        mockMvc.perform(multipart("/me/avatar"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail.code").value("BUYER_UNAUTHORIZED"));
    }

    @Test
    void uploadBuyerAvatarRejectsUnsupportedMimeType() throws Exception {
        MockMultipartFile avatar = new MockMultipartFile(
                "file",
                "avatar.txt",
                "text/plain",
                "not-an-image".getBytes()
        );

        mockMvc.perform(multipart("/me/avatar")
                        .file(avatar)
                        .header("Authorization", "Bearer " + loginAsBuyer()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail.code").value("BUYER_AVATAR_INVALID_TYPE"));
    }

    @Test
    void uploadBuyerAvatarRejectsOversizedFile() throws Exception {
        MockMultipartFile avatar = new MockMultipartFile(
                "file",
                "avatar.png",
                "image/png",
                oversizedAvatarBytes()
        );

        mockMvc.perform(multipart("/me/avatar")
                        .file(avatar)
                        .header("Authorization", "Bearer " + loginAsBuyer()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail.code").value("BUYER_AVATAR_TOO_LARGE"));
    }

    @Test
    void uploadBuyerAvatarReturnsNotFoundWhenBuyerProfileMissing() throws Exception {
        MockMultipartFile avatar = new MockMultipartFile(
                "file",
                "avatar.png",
                "image/png",
                new byte[] {1, 2, 3}
        );

        mockMvc.perform(multipart("/me/avatar")
                        .file(avatar))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail.code").value("BUYER_UNAUTHORIZED"));
    }

    private byte[] oversizedAvatarBytes() {
        byte[] bytes = new byte[5 * 1024 * 1024 + 1];
        Arrays.fill(bytes, (byte) 1);
        return bytes;
    }

    private String loginAsBuyer() throws Exception {
        String responseBody = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/auth/login")
                        .contentType("application/json")
                        .content("""
                                {
                                  "phoneNumber": "024 567 8901",
                                  "password": "Passw0rd!"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        int start = responseBody.indexOf("\"accessToken\":\"");
        int valueStart = start + "\"accessToken\":\"".length();
        int valueEnd = responseBody.indexOf('"', valueStart);
        return responseBody.substring(valueStart, valueEnd);
    }
}
