package com.zokomart.backend;

import cn.dev33.satoken.stp.StpUtil;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminCategoryTreeControllerTest {

    private static final String PLATFORM_ADMIN_ID = "admin-platform-category-tree-001";
    private static final String ROOT_CATEGORY_ID = "f6f2c39a-1438-4e90-bcb2-bcb4db719001";
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
    void platformAdminCanReadCategoryTree() throws Exception {
        seedCategoryImage(ROOT_CATEGORY_ID);
        mockMvc.perform(get("/admin/categories/tree").cookie(loginAsPlatformAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(ROOT_CATEGORY_ID))
                .andExpect(jsonPath("$[0].path").value(org.hamcrest.Matchers.startsWith("/")))
                .andExpect(jsonPath("$[0].imageUrl").isNotEmpty())
                .andExpect(jsonPath("$[0].children").isArray());
    }

    @Test
    void platformAdminMustUploadImageWhenCreatingRootCategory() throws Exception {
        mockMvc.perform(multipart("/admin/categories")
                        .cookie(loginAsPlatformAdmin())
                        .param("name", "Electronics")
                        .param("code", "electronics")
                        .param("sortOrder", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail.code").value("CATEGORY_IMAGE_REQUIRED"));
    }

    @Test
    void platformAdminCanCreateRootCategoryWithImage() throws Exception {
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "electronics.png",
                "image/png",
                pngBytes()
        );

        mockMvc.perform(multipart("/admin/categories")
                        .file(image)
                        .cookie(loginAsPlatformAdmin())
                        .param("name", "Electronics")
                        .param("code", "electronics")
                        .param("sortOrder", "10"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("electronics"))
                .andExpect(jsonPath("$.imageUrl").isNotEmpty())
                .andExpect(jsonPath("$.children").isArray());
    }

    @Test
    void platformAdminCanCreateCategoryChildWithImage() throws Exception {
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "android-phones.png",
                "image/png",
                pngBytes()
        );

        mockMvc.perform(multipart("/admin/categories/{categoryId}/children", ROOT_CATEGORY_ID)
                        .file(image)
                        .cookie(loginAsPlatformAdmin())
                        .param("name", "Android Phones")
                        .param("code", "android-phones")
                        .param("sortOrder", "20"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.parentId").value(ROOT_CATEGORY_ID))
                .andExpect(jsonPath("$.imageUrl").isNotEmpty());
    }

    @Test
    void platformAdminMustUploadImageWhenCreatingChildCategory() throws Exception {
        mockMvc.perform(multipart("/admin/categories/{categoryId}/children", ROOT_CATEGORY_ID)
                        .cookie(loginAsPlatformAdmin())
                        .param("name", "Android Phones")
                        .param("code", "android-phones")
                        .param("sortOrder", "20"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail.code").value("CATEGORY_IMAGE_REQUIRED"));
    }

    @Test
    void platformAdminRejectsInvalidCategoryImageMimeType() throws Exception {
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "electronics.txt",
                "text/plain",
                "not-an-image".getBytes()
        );

        mockMvc.perform(multipart("/admin/categories")
                        .file(image)
                        .cookie(loginAsPlatformAdmin())
                        .param("name", "Electronics")
                        .param("code", "electronics")
                        .param("sortOrder", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail.code").value("CATEGORY_IMAGE_INVALID_TYPE"));
    }

    @Test
    void platformAdminRejectsOversizedCategoryImage() throws Exception {
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "electronics.png",
                "image/png",
                oversizedPngBytes()
        );

        mockMvc.perform(multipart("/admin/categories")
                        .file(image)
                        .cookie(loginAsPlatformAdmin())
                        .param("name", "Electronics")
                        .param("code", "electronics")
                        .param("sortOrder", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail.code").value("CATEGORY_IMAGE_TOO_LARGE"));
    }

    private Cookie loginAsPlatformAdmin() {
        seedAdminUser(PLATFORM_ADMIN_ID, "platform.category.tree", "Platform Category Tree Admin", "PLATFORM_ADMIN");
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

    private void seedCategoryImage(String categoryId) {
        jdbcTemplate.update(
                "UPDATE categories SET image_url = ?, image_storage_key = ? WHERE id = ?",
                "/public/uploads/categories/seed.png",
                "categories/seed.png",
                categoryId
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
