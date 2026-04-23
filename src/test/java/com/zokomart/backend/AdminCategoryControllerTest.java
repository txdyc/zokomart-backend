package com.zokomart.backend;

import cn.dev33.satoken.stp.StpUtil;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Arrays;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminCategoryControllerTest {

    private static final String ADMIN_ID = "admin-001";
    private static final String CATEGORY_ID = "f6f2c39a-1438-4e90-bcb2-bcb4db719001";
    private static final Path TEST_UPLOAD_ROOT = Path.of("target", "test-uploads");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanupUploads() {
        deleteDirectory(TEST_UPLOAD_ROOT);
    }

    @Test
    void listAdminCategoriesRequiresLogin() throws Exception {
        mockMvc.perform(get("/admin/categories"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail.code").value("ADMIN_UNAUTHORIZED"));
    }

    @Test
    void listAdminCategoriesReturnsSeededCategory() throws Exception {
        mockMvc.perform(get("/admin/categories").cookie(loginAsPlatformAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(CATEGORY_ID))
                .andExpect(jsonPath("$.items[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.items[0].productCount").value(1));
    }

    @Test
    void getCategoryDetailReturnsImageUrl() throws Exception {
        seedExistingImage();
        mockMvc.perform(get("/admin/categories/{categoryId}", CATEGORY_ID)
                        .cookie(loginAsPlatformAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(CATEGORY_ID))
                .andExpect(jsonPath("$.imageUrl").isNotEmpty());
    }

    @Test
    void updateCategoryRequiresImageWhenCurrentCategoryHasNoImage() throws Exception {
        jdbcTemplate.update("UPDATE categories SET image_url = NULL, image_storage_key = NULL WHERE id = ?", CATEGORY_ID);

        mockMvc.perform(multipart("/admin/categories/{categoryId}", CATEGORY_ID)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        })
                        .cookie(loginAsPlatformAdmin())
                        .param("name", "Mobile Phones")
                        .param("code", "mobile-phones")
                        .param("sortOrder", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail.code").value("CATEGORY_IMAGE_REQUIRED"));
    }

    @Test
    void updateCategoryCanKeepExistingImageWhenSavingOtherFields() throws Exception {
        jdbcTemplate.update(
                "UPDATE categories SET image_url = ?, image_storage_key = ? WHERE id = ?",
                "/public/uploads/categories/mobile-phones.png",
                "categories/mobile-phones.png",
                CATEGORY_ID
        );

        mockMvc.perform(multipart("/admin/categories/{categoryId}", CATEGORY_ID)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        })
                        .cookie(loginAsPlatformAdmin())
                        .param("name", "Mobile Phones Updated")
                        .param("code", "mobile-phones")
                        .param("sortOrder", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Mobile Phones Updated"))
                .andExpect(jsonPath("$.imageUrl").isNotEmpty());
    }

    @Test
    void updateCategoryCanReplaceExistingImage() throws Exception {
        seedExistingImage();
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "replacement.png",
                "image/png",
                pngBytes()
        );

        mockMvc.perform(multipart("/admin/categories/{categoryId}", CATEGORY_ID)
                        .file(image)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        })
                        .cookie(loginAsPlatformAdmin())
                        .param("name", "Mobile Phones")
                        .param("code", "mobile-phones")
                        .param("sortOrder", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imageUrl").isNotEmpty());
    }

    @Test
    void updateCategoryCanReplaceExistingImageViaPostMultipartUpdate() throws Exception {
        seedExistingImage();
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "replacement-post.png",
                "image/png",
                pngBytes()
        );

        mockMvc.perform(multipart("/admin/categories/{categoryId}", CATEGORY_ID)
                        .file(image)
                        .cookie(loginAsPlatformAdmin())
                        .param("name", "Mobile Phones Post")
                        .param("code", "mobile-phones")
                        .param("sortOrder", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Mobile Phones Post"))
                .andExpect(jsonPath("$.imageUrl").isNotEmpty());
    }

    @Test
    void updateCategoryRejectsReplacementImageWithInvalidMimeType() throws Exception {
        seedExistingImage();
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "replacement.txt",
                "text/plain",
                "not-an-image".getBytes()
        );

        mockMvc.perform(multipart("/admin/categories/{categoryId}", CATEGORY_ID)
                        .file(image)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        })
                        .cookie(loginAsPlatformAdmin())
                        .param("name", "Mobile Phones")
                        .param("code", "mobile-phones")
                        .param("sortOrder", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail.code").value("CATEGORY_IMAGE_INVALID_TYPE"));
    }

    @Test
    void updateCategoryRejectsReplacementImageThatIsTooLarge() throws Exception {
        seedExistingImage();
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "replacement.png",
                "image/png",
                oversizedPngBytes()
        );

        mockMvc.perform(multipart("/admin/categories/{categoryId}", CATEGORY_ID)
                        .file(image)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        })
                        .cookie(loginAsPlatformAdmin())
                        .param("name", "Mobile Phones")
                        .param("code", "mobile-phones")
                        .param("sortOrder", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail.code").value("CATEGORY_IMAGE_TOO_LARGE"));
    }

    @Test
    void deactivateCategoryWritesAuditLog() throws Exception {
        mockMvc.perform(
                        post("/admin/categories/{id}/deactivate", CATEGORY_ID)
                                .cookie(loginAsPlatformAdmin())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "reason": "Temporarily hidden for cleanup"
                                        }
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(CATEGORY_ID))
                .andExpect(jsonPath("$.status").value("INACTIVE"));

        Integer logs = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM admin_action_logs WHERE entity_type = 'CATEGORY' AND entity_id = ?",
                Integer.class,
                CATEGORY_ID
        );
        assertThat(logs).isEqualTo(1);
    }

    private Cookie loginAsPlatformAdmin() {
        seedAdminUser(ADMIN_ID, "platform.category.ops", "Platform Category Admin", "PLATFORM_ADMIN");
        return new Cookie("satoken", StpUtil.createLoginSession(ADMIN_ID));
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

    private byte[] pngBytes() {
        return Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/a9sAAAAASUVORK5CYII=");
    }

    private byte[] oversizedPngBytes() {
        byte[] bytes = new byte[1024 * 1024 + 1];
        Arrays.fill(bytes, (byte) 1);
        return bytes;
    }

    private void seedExistingImage() {
        jdbcTemplate.update(
                "UPDATE categories SET image_url = ?, image_storage_key = ? WHERE id = ?",
                "/public/uploads/categories/mobile-phones.png",
                "categories/mobile-phones.png",
                CATEGORY_ID
        );
    }

    private void deleteDirectory(Path root) {
        Path normalized = root.toAbsolutePath().normalize();
        if (!Files.exists(normalized)) {
            return;
        }
        try (var stream = Files.walk(normalized)) {
            stream.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ignored) {
                            // Best-effort cleanup for test isolation.
                        }
                    });
        } catch (IOException ignored) {
            // Best-effort cleanup for test isolation.
        }
    }
}
