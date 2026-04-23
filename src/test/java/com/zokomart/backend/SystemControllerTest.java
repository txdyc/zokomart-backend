package com.zokomart.backend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SystemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthcheckReturnsOk() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    void openApiLikeRouteSkeletonExists() throws Exception {
        mockMvc.perform(get("/api/system/routes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0]").value("/products"))
                .andExpect(jsonPath("$.items[1]").value("/cart"))
                .andExpect(jsonPath("$.items[2]").value("/orders"))
                .andExpect(jsonPath("$.items[3]").value("/merchant/orders"));
    }
}
