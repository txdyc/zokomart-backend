package com.zokomart.backend;

import com.zokomart.backend.catalog.stats.CategoryStatsService;
import com.zokomart.backend.catalog.stats.dto.TopCategoryResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CategoryControllerTest {

    private static final String CATEGORY_ID = "f6f2c39a-1438-4e90-bcb2-bcb4db719001";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CategoryStatsService categoryStatsService;

    @Test
    void topCategoriesEndpointReturnsRankedCategories() throws Exception {
        when(categoryStatsService.getTopCategories(2)).thenReturn(List.of(
                new TopCategoryResponse(
                        CATEGORY_ID,
                        "CAT-PHONES",
                        "phones",
                        "Mobile Phones",
                        "/public/uploads/categories/mobile-phones.png",
                        42L
                )
        ));

        mockMvc.perform(get("/api/categories/top").param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(CATEGORY_ID))
                .andExpect(jsonPath("$[0].name").value("Mobile Phones"))
                .andExpect(jsonPath("$[0].viewCount").value(42));

        verify(categoryStatsService).getTopCategories(2);
    }

    @Test
    void categoryViewEndpointTracksClick() throws Exception {
        mockMvc.perform(post("/api/categories/{categoryId}/view", CATEGORY_ID))
                .andExpect(status().isNoContent());

        verify(categoryStatsService).incrementView(CATEGORY_ID);
    }

    @Test
    void categoryFilteredProductListTracksCategoryViewAfterSuccessfulValidation() throws Exception {
        mockMvc.perform(get("/products").param("categoryId", CATEGORY_ID))
                .andExpect(status().isOk());

        verify(categoryStatsService).incrementView(CATEGORY_ID);
    }
}
