package com.zokomart.backend;

import cn.dev33.satoken.stp.StpUtil;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.mock.web.MockMultipartFile;

import java.util.UUID;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminBrandControllerTest {

    private static final String PLATFORM_ADMIN_ID = "admin-platform-brand-001";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void platformAdminMustUploadImageWhenCreatingBrand() throws Exception {
        mockMvc.perform(multipart("/admin/brands")
                        .cookie(loginAsPlatformAdmin())
                        .param("name", "Itel")
                        .param("code", "itel"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail.code").value("BRAND_IMAGE_REQUIRED"));
    }

    @Test
    void platformAdminCanCreateBrandWithImage() throws Exception {
        mockMvc.perform(multipart("/admin/brands")
                        .file(pngFile("itel.png"))
                        .cookie(loginAsPlatformAdmin())
                        .param("name", "Itel")
                        .param("code", "itel-test"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Itel"))
                .andExpect(jsonPath("$.sourceType").value("PLATFORM"))
                .andExpect(jsonPath("$.imageUrl").isNotEmpty());
    }

    @Test
    void platformAdminCanReplaceBrandImage() throws Exception {
        String brandId = seedBrandWithImage("tecno-updatable", "/public/uploads/brands/old.png", "brands/old.png");

        mockMvc.perform(multipart("/admin/brands/{brandId}", brandId)
                        .file(pngFile("new.png"))
                        .cookie(loginAsPlatformAdmin())
                        .param("name", "Tecno Updated")
                        .param("code", "tecno-updatable")
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Tecno Updated"))
                .andExpect(jsonPath("$.imageUrl").isNotEmpty());
    }

    @Test
    void platformAdminCanReplaceBrandImageViaPostMultipartUpdate() throws Exception {
        String brandId = seedBrandWithImage("tecno-post-updatable", "/public/uploads/brands/old-post.png", "brands/old-post.png");

        mockMvc.perform(multipart("/admin/brands/{brandId}", brandId)
                        .file(pngFile("post-new.png"))
                        .cookie(loginAsPlatformAdmin())
                        .param("name", "Tecno Post Updated")
                        .param("code", "tecno-post-updatable"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Tecno Post Updated"))
                .andExpect(jsonPath("$.imageUrl").isNotEmpty());
    }

    @Test
    void platformAdminCanListBrands() throws Exception {
        mockMvc.perform(get("/admin/brands").cookie(loginAsPlatformAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items[0].imageUrl").value(nullValue()));
    }

    private Cookie loginAsPlatformAdmin() {
        seedAdminUser(PLATFORM_ADMIN_ID, "platform.brand.ops", "Platform Brand Admin", "PLATFORM_ADMIN");
        return new Cookie("satoken", StpUtil.createLoginSession(PLATFORM_ADMIN_ID));
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

    private String seedBrandWithImage(String code, String imageUrl, String imageStorageKey) {
        String brandId = UUID.randomUUID().toString();
        jdbcTemplate.update(
                """
                        INSERT INTO brands (
                            id,
                            name,
                            code,
                            status,
                            source_type,
                            approved_by_admin_id,
                            image_storage_key,
                            image_url,
                            image_content_type,
                            image_size_bytes,
                            image_original_filename,
                            created_at,
                            updated_at
                        )
                        VALUES (?, ?, ?, 'APPROVED', 'PLATFORM', ?, ?, ?, 'image/png', 32, 'seed.png', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        """,
                brandId,
                "Seed Brand",
                code,
                "00000000-0000-0000-0000-000000000001",
                imageStorageKey,
                imageUrl
        );
        return brandId;
    }

    private MockMultipartFile pngFile(String filename) {
        return new MockMultipartFile("image", filename, "image/png", new byte[] {1, 2, 3});
    }
}
