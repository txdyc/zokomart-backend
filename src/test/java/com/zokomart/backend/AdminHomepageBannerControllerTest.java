package com.zokomart.backend;

import cn.dev33.satoken.stp.StpUtil;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminHomepageBannerControllerTest {

    private static final String PLATFORM_ADMIN_ID = "admin-homepage-banner-001";
    private static final String SEEDED_PRODUCT_ID = "6db6eb8a-c00e-4c85-8bd8-6ae76fd4f8d5";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void platformAdminCanCreateProductBannerWithImage() throws Exception {
        mockMvc.perform(multipart("/admin/homepage-banners")
                        .file(imageFile("banner.png"))
                        .cookie(loginAsPlatformAdmin())
                        .param("title", "Tecno Hero")
                        .param("targetType", "PRODUCT_DETAIL")
                        .param("targetProductId", SEEDED_PRODUCT_ID)
                        .param("sortOrder", "0")
                        .param("enabled", "true"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Tecno Hero"))
                .andExpect(jsonPath("$.targetType").value("PRODUCT_DETAIL"))
                .andExpect(jsonPath("$.imageUrl").isNotEmpty())
                .andExpect(jsonPath("$.enabled").value(true));
    }

    @Test
    void cannotEnableSixthHomepageBanner() throws Exception {
        for (int index = 0; index < 5; index++) {
            seedHomepageBanner("seed-" + index, true, index);
        }

        mockMvc.perform(multipart("/admin/homepage-banners")
                        .file(imageFile("banner-limit.png"))
                        .cookie(loginAsPlatformAdmin())
                        .param("title", "Overflow Banner")
                        .param("targetType", "ACTIVITY_PAGE")
                        .param("targetActivityKey", "easter-sale")
                        .param("sortOrder", "10")
                        .param("enabled", "true"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail.code").value("HOMEPAGE_BANNER_ENABLED_LIMIT_EXCEEDED"));
    }

    @Test
    void platformAdminCanListHomepageBanners() throws Exception {
        seedHomepageBanner("list-banner", false, 3);

        mockMvc.perform(get("/admin/homepage-banners").cookie(loginAsPlatformAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items[0].title").isNotEmpty());
    }

    private Cookie loginAsPlatformAdmin() {
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
                PLATFORM_ADMIN_ID,
                "platform.banner.ops",
                "Platform Banner Admin",
                "{noop}not-used-in-this-test",
                "PLATFORM_ADMIN",
                "ACTIVE"
        );
        return new Cookie("satoken", StpUtil.createLoginSession(PLATFORM_ADMIN_ID));
    }

    private MockMultipartFile imageFile(String filename) {
        return new MockMultipartFile("image", filename, "image/png", new byte[] {1, 2, 3});
    }

    private void seedHomepageBanner(String title, boolean enabled, int sortOrder) {
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
                        VALUES (?, ?, ?, ?, 'image/png', 32, 'seed.png', 'ACTIVITY_PAGE', 'seed-activity', ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        """,
                UUID.randomUUID().toString(),
                title,
                "homepage-banners/seed.png",
                "/public/uploads/homepage-banners/seed.png",
                sortOrder,
                enabled
        );
    }
}
