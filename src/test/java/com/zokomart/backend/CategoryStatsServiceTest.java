package com.zokomart.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zokomart.backend.catalog.entity.CategoryEntity;
import com.zokomart.backend.catalog.mapper.CategoryMapper;
import com.zokomart.backend.catalog.stats.CategoryStatsMapper;
import com.zokomart.backend.catalog.stats.CategoryStatsRedisKeyScanner;
import com.zokomart.backend.catalog.stats.CategoryStatsService;
import com.zokomart.backend.catalog.stats.dto.TopCategoryResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CategoryStatsServiceTest {

    private static final String CATEGORY_ID = "f6f2c39a-1438-4e90-bcb2-bcb4db719001";

    private CategoryStatsMapper categoryStatsMapper;
    private CategoryMapper categoryMapper;
    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private CategoryStatsRedisKeyScanner keyScanner;
    private CategoryStatsService categoryStatsService;

    @BeforeEach
    void setUp() {
        categoryStatsMapper = mock(CategoryStatsMapper.class);
        categoryMapper = mock(CategoryMapper.class);
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        keyScanner = mock(CategoryStatsRedisKeyScanner.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        categoryStatsService = new CategoryStatsService(
                categoryStatsMapper,
                categoryMapper,
                redisTemplate,
                new ObjectMapper(),
                keyScanner,
                Clock.fixed(Instant.parse("2026-04-30T12:00:00Z"), ZoneOffset.UTC)
        );
    }

    @Test
    void incrementViewUsesRedisAtomicCountersForCumulativeDailyAndWeeklyStats() {
        when(categoryMapper.selectById(CATEGORY_ID)).thenReturn(activeCategory(CATEGORY_ID, "Mobile Phones"));

        categoryStatsService.incrementView(CATEGORY_ID);

        verify(valueOperations).increment("category:view:" + CATEGORY_ID);
        verify(valueOperations).increment("category:view:daily:2026-04-30:" + CATEGORY_ID);
        verify(valueOperations).increment("category:view:weekly:2026-W18:" + CATEGORY_ID);
    }

    @Test
    void getTopCategoriesMergesDatabaseTotalsWithUnsyncedRedisDeltasAndCachesResult() {
        when(valueOperations.get("category:top:2")).thenReturn(null);
        when(keyScanner.scanCumulativeViewKeys()).thenReturn(List.of("category:view:category-hot"));
        when(valueOperations.get("category:view:category-hot")).thenReturn("25");
        when(categoryStatsMapper.selectTopActiveCategories(4)).thenReturn(List.of(
                new CategoryStatsMapper.TopCategoryRow(
                        "category-steady",
                        "CAT-STEADY",
                        "steady",
                        "Steady Category",
                        "/images/steady.png",
                        20L
                )
        ));
        when(categoryStatsMapper.selectActiveCategoriesByIds(List.of("category-hot"))).thenReturn(List.of(
                new CategoryStatsMapper.TopCategoryRow(
                        "category-hot",
                        "CAT-HOT",
                        "hot",
                        "Hot Category",
                        "/images/hot.png",
                        1L
                )
        ));

        List<TopCategoryResponse> response = categoryStatsService.getTopCategories(2);

        assertThat(response).extracting(TopCategoryResponse::id)
                .containsExactly("category-hot", "category-steady");
        assertThat(response).extracting(TopCategoryResponse::viewCount)
                .containsExactly(26L, 20L);
        verify(valueOperations).set(eq("category:top:2"), anyString(), eq(CategoryStatsService.TOP_CACHE_TTL));
    }

    @Test
    void syncRedisDeltasPersistsExactAmountAndPreservesConcurrentIncrements() {
        when(keyScanner.scanCumulativeViewKeys()).thenReturn(List.of("category:view:" + CATEGORY_ID));
        when(valueOperations.get("category:view:" + CATEGORY_ID)).thenReturn("5");
        when(valueOperations.decrement("category:view:" + CATEGORY_ID, 5L)).thenReturn(2L);
        when(categoryStatsMapper.incrementExisting(CATEGORY_ID, 5L)).thenReturn(1);

        int synced = categoryStatsService.syncRedisDeltasToDatabase();

        assertThat(synced).isEqualTo(1);
        verify(categoryStatsMapper).incrementExisting(CATEGORY_ID, 5L);
        verify(redisTemplate, never()).delete("category:view:" + CATEGORY_ID);
    }

    @Test
    void syncRedisDeltasRetriesUpdateWhenConcurrentInsertWinsRace() {
        when(keyScanner.scanCumulativeViewKeys()).thenReturn(List.of("category:view:" + CATEGORY_ID));
        when(valueOperations.get("category:view:" + CATEGORY_ID)).thenReturn("5");
        when(valueOperations.decrement("category:view:" + CATEGORY_ID, 5L)).thenReturn(0L);
        when(categoryStatsMapper.incrementExisting(CATEGORY_ID, 5L)).thenReturn(0, 1);
        when(categoryStatsMapper.insertInitial(CATEGORY_ID, 5L)).thenThrow(new DuplicateKeyException("race"));

        int synced = categoryStatsService.syncRedisDeltasToDatabase();

        assertThat(synced).isEqualTo(1);
        verify(categoryStatsMapper).insertInitial(CATEGORY_ID, 5L);
        verify(redisTemplate).delete("category:view:" + CATEGORY_ID);
    }

    private CategoryEntity activeCategory(String categoryId, String name) {
        CategoryEntity category = new CategoryEntity();
        category.setId(categoryId);
        category.setName(name);
        category.setStatus("ACTIVE");
        return category;
    }
}
