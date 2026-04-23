package com.zokomart.backend;

import cn.dev33.satoken.stp.StpUtil;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminAttributeControllerTest {

    private static final String PLATFORM_ADMIN_ID = "admin-platform-attribute-001";
    private static final String CATEGORY_ID = "f6f2c39a-1438-4e90-bcb2-bcb4db719001";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void platformAdminCanCreateAttributeTemplate() throws Exception {
        mockMvc.perform(post("/admin/attributes")
                        .cookie(loginAsPlatformAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Storage",
                                  "code": "storage",
                                  "type": "TEXT",
                                  "categoryId": "%s",
                                  "filterable": true,
                                  "searchable": true,
                                  "required": true,
                                  "customAllowed": false
                                }
                                """.formatted(CATEGORY_ID)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Storage"))
                .andExpect(jsonPath("$.categoryId").value(CATEGORY_ID))
                .andExpect(jsonPath("$.filterable").value(true))
                .andExpect(jsonPath("$.searchable").value(true));
    }

    @Test
    void platformAdminCanResolveCategoryAttributes() throws Exception {
        mockMvc.perform(get("/admin/categories/{categoryId}/resolved-attributes", CATEGORY_ID)
                        .cookie(loginAsPlatformAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryId").value(CATEGORY_ID))
                .andExpect(jsonPath("$.items").isArray());
    }

    private Cookie loginAsPlatformAdmin() {
        seedAdminUser(PLATFORM_ADMIN_ID, "platform.attribute.ops", "Platform Attribute Admin", "PLATFORM_ADMIN");
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
}
