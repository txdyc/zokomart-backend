package com.zokomart.backend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class HomeBannerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void storefrontReturnsOnlyEnabledAndTargetValidBanners() throws Exception {
        seedCampaignBanner("Visible Campaign", true, 0, "visible-campaign");
        seedCampaignBanner("Disabled Campaign", false, 1, "disabled-campaign");

        mockMvc.perform(get("/home/banners"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].title").value("Visible Campaign"))
                .andExpect(jsonPath("$[0].targetHref").value("/campaigns/visible-campaign"))
                .andExpect(jsonPath("$[1]").doesNotExist());
    }

    private void seedCampaignBanner(String title, boolean enabled, int sortOrder, String campaignKey) {
        jdbcTemplate.update(
                """
                        INSERT INTO homepage_banners (
                            id,
                            title,
                            image_storage_key,
                            image_url,
                            image_content_type,
                            image_size_bytes,
                            image_original_filename,
                            target_type,
                            target_activity_key,
                            sort_order,
                            enabled,
                            created_at,
                            updated_at
                        )
                        VALUES (?, ?, ?, ?, 'image/png', 32, 'seed.png', 'ACTIVITY_PAGE', ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        """,
                UUID.randomUUID().toString(),
                title,
                "homepage-banners/" + campaignKey + ".png",
                "/public/uploads/homepage-banners/" + campaignKey + ".png",
                campaignKey,
                sortOrder,
                enabled
        );
    }
}
