package com.zokomart.backend;

import com.zokomart.backend.catalog.stats.CategoryStatsRedisKeyScanner;
import com.zokomart.backend.catalog.stats.CategoryStatsService;
import com.zokomart.backend.catalog.stats.dto.TopCategoryResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CategoryStatsPersistenceTest {

    private static final String STEADY_CATEGORY_ID = "category-stats-steady-001";
    private static final String HOT_CATEGORY_ID = "category-stats-hot-001";

    @Autowired
    private CategoryStatsService categoryStatsService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private StringRedisTemplate redisTemplate;

    @MockBean
    private CategoryStatsRedisKeyScanner keyScanner;

    @Test
    void topCategoriesUseMyBatisQueriesAndMergeRedisDeltas() {
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("category:top:2")).thenReturn(null);
        when(keyScanner.scanCumulativeViewKeys()).thenReturn(List.of("category:view:" + HOT_CATEGORY_ID));
        when(valueOperations.get("category:view:" + HOT_CATEGORY_ID)).thenReturn("7");

        seedCategory(STEADY_CATEGORY_ID, "steady-category", "Steady Category", 1);
        seedCategory(HOT_CATEGORY_ID, "hot-category", "Hot Category", 2);
        seedCategoryStats(STEADY_CATEGORY_ID, 4L);

        List<TopCategoryResponse> response = categoryStatsService.getTopCategories(2);

        assertThat(response).extracting(TopCategoryResponse::id)
                .containsExactly(HOT_CATEGORY_ID, STEADY_CATEGORY_ID);
        assertThat(response).extracting(TopCategoryResponse::viewCount)
                .containsExactly(7L, 4L);
    }

    @Test
    void syncRedisDeltasUsesUpsertIncrementSql() {
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(keyScanner.scanCumulativeViewKeys()).thenReturn(List.of("category:view:" + STEADY_CATEGORY_ID));
        when(valueOperations.get("category:view:" + STEADY_CATEGORY_ID)).thenReturn("6");
        when(valueOperations.decrement("category:view:" + STEADY_CATEGORY_ID, 6L)).thenReturn(0L);

        seedCategory(STEADY_CATEGORY_ID, "steady-category", "Steady Category", 1);
        seedCategoryStats(STEADY_CATEGORY_ID, 4L);

        int syncedCount = categoryStatsService.syncRedisDeltasToDatabase();

        Long viewCount = jdbcTemplate.queryForObject(
                "SELECT view_count FROM category_stats WHERE category_id = ?",
                Long.class,
                STEADY_CATEGORY_ID
        );
        assertThat(syncedCount).isEqualTo(1);
        assertThat(viewCount).isEqualTo(10L);
    }

    private void seedCategory(String categoryId, String code, String name, int sortOrder) {
        jdbcTemplate.update(
                """
                        INSERT INTO categories (
                            id,
                            category_code,
                            code,
                            name,
                            description,
                            parent_id,
                            path,
                            level,
                            sort_order,
                            status,
                            created_at,
                            updated_at
                        )
                        VALUES (?, ?, ?, ?, NULL, NULL, ?, 1, ?, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        """,
                categoryId,
                "CAT-" + code.toUpperCase().replace("-", "_"),
                code,
                name,
                "/" + code,
                sortOrder
        );
    }

    private void seedCategoryStats(String categoryId, long viewCount) {
        jdbcTemplate.update(
                """
                        INSERT INTO category_stats (id, category_id, view_count, created_at, updated_at)
                        VALUES (?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        """,
                categoryId,
                categoryId,
                viewCount
        );
    }
}
