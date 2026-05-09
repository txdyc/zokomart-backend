package com.zokomart.backend;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class BuyerAuthControllerTest {

    private static final String BUYER_ID = "00000000-0000-0000-0000-000000000101";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void loginReturnsAccessTokenAndBuyerSummary() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "phoneNumber": "024 567 8901",
                                  "password": "Passw0rd!"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("satoken"))
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.user.buyerId").value(BUYER_ID))
                .andExpect(jsonPath("$.user.phoneNumber").value("+233 24 567 8901"))
                .andExpect(jsonPath("$.user.fullName").value("Abena Mensah"));
    }

    @Test
    void loginAllowsLanFrontendDevOrigin() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .header("Origin", "http://192.168.3.21:3000")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "phoneNumber": "024 567 8901",
                                  "password": "Passw0rd!"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://192.168.3.21:3000"))
                .andExpect(jsonPath("$.user.buyerId").value(BUYER_ID));
    }

    @Test
    void loginRejectsInvalidPassword() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "phoneNumber": "024 567 8901",
                                  "password": "wrong-password"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail.code").value("BUYER_LOGIN_INVALID"));
    }

    @Test
    void loginRejectsDisabledBuyerAccount() throws Exception {
        jdbcTemplate.update(
                "UPDATE buyer_auth_accounts SET status = 'DISABLED' WHERE buyer_id = ?",
                BUYER_ID
        );

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "phoneNumber": "024 567 8901",
                                  "password": "Passw0rd!"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail.code").value("BUYER_LOGIN_DISABLED"));
    }

    @Test
    void currentBuyerRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail.code").value("BUYER_UNAUTHORIZED"));
    }

    @Test
    void logoutInvalidatesBuyerSession() throws Exception {
        String token = loginAsBuyer();

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail.code").value("BUYER_UNAUTHORIZED"));
    }

    private String loginAsBuyer() throws Exception {
        String token = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
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

        int start = token.indexOf("\"accessToken\":\"");
        assertThat(start).isGreaterThanOrEqualTo(0);
        int valueStart = start + "\"accessToken\":\"".length();
        int valueEnd = token.indexOf('"', valueStart);
        return token.substring(valueStart, valueEnd);
    }
}
