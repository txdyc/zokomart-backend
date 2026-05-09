package com.zokomart.backend.catalog.stats;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zokomart.backend.catalog.entity.CategoryEntity;
import com.zokomart.backend.catalog.mapper.CategoryMapper;
import com.zokomart.backend.catalog.stats.dto.TopCategoryResponse;
import com.zokomart.backend.common.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class CategoryStatsService {

    public static final Duration TOP_CACHE_TTL = Duration.ofSeconds(60);
    private static final Logger log = LoggerFactory.getLogger(CategoryStatsService.class);
    private static final TypeReference<List<TopCategoryResponse>> TOP_CATEGORY_LIST_TYPE = new TypeReference<>() {
    };
    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 50;

    private final CategoryStatsMapper categoryStatsMapper;
    private final CategoryMapper categoryMapper;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final CategoryStatsRedisKeyScanner keyScanner;
    private final Clock clock;

    @Autowired
    public CategoryStatsService(
            CategoryStatsMapper categoryStatsMapper,
            CategoryMapper categoryMapper,
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            CategoryStatsRedisKeyScanner keyScanner
    ) {
        this(categoryStatsMapper, categoryMapper, redisTemplate, objectMapper, keyScanner, Clock.systemUTC());
    }

    public CategoryStatsService(
            CategoryStatsMapper categoryStatsMapper,
            CategoryMapper categoryMapper,
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            CategoryStatsRedisKeyScanner keyScanner,
            Clock clock
    ) {
        this.categoryStatsMapper = categoryStatsMapper;
        this.categoryMapper = categoryMapper;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.keyScanner = keyScanner;
        this.clock = clock;
    }

    public void incrementView(String categoryId) {
        String normalizedCategoryId = requireActiveCategoryId(categoryId);
        LocalDate today = LocalDate.now(clock);
        redisTemplate.opsForValue().increment(CategoryStatsRedisKeys.cumulativeView(normalizedCategoryId));
        redisTemplate.opsForValue().increment(CategoryStatsRedisKeys.dailyView(today, normalizedCategoryId));
        redisTemplate.opsForValue().increment(CategoryStatsRedisKeys.weeklyView(today, normalizedCategoryId));
    }

    public List<TopCategoryResponse> getTopCategories(Integer requestedLimit) {
        int limit = normalizeLimit(requestedLimit);
        String cacheKey = CategoryStatsRedisKeys.top(limit);
        List<TopCategoryResponse> cached = readCachedTopCategories(cacheKey);
        if (cached != null) {
            return cached;
        }

        List<TopCategoryResponse> response = computeTopCategories(limit);
        cacheTopCategories(cacheKey, response);
        return response;
    }

    public int syncRedisDeltasToDatabase() {
        int syncedCount = 0;
        for (String key : keyScanner.scanCumulativeViewKeys()) {
            long delta = parsePositiveLong(redisTemplate.opsForValue().get(key)).orElse(0L);
            if (delta <= 0) {
                continue;
            }
            String categoryId = CategoryStatsRedisKeys.categoryIdFromCumulativeKey(key);
            try {
                incrementPersistentViewCount(categoryId, delta);
                Long remaining = redisTemplate.opsForValue().decrement(key, delta);
                if (remaining != null && remaining <= 0) {
                    redisTemplate.delete(key);
                }
                syncedCount++;
            } catch (RuntimeException exception) {
                log.warn("Failed to sync category view counter for categoryId={}", categoryId, exception);
            }
        }
        return syncedCount;
    }

    private void incrementPersistentViewCount(String categoryId, long delta) {
        int updatedRows = categoryStatsMapper.incrementExisting(categoryId, delta);
        if (updatedRows > 0) {
            return;
        }
        try {
            categoryStatsMapper.insertInitial(categoryId, delta);
        } catch (DuplicateKeyException exception) {
            categoryStatsMapper.incrementExisting(categoryId, delta);
        }
    }

    private List<TopCategoryResponse> computeTopCategories(int limit) {
        Map<String, Candidate> candidates = new LinkedHashMap<>();
        int databaseCandidateLimit = Math.min(MAX_LIMIT, Math.max(limit * 2, limit));
        for (CategoryStatsMapper.TopCategoryRow row : categoryStatsMapper.selectTopActiveCategories(databaseCandidateLimit)) {
            candidates.put(row.id(), Candidate.from(row));
        }

        Map<String, Long> redisDeltas = readRedisDeltas();
        List<String> missingRedisCategoryIds = redisDeltas.keySet().stream()
                .filter(categoryId -> !candidates.containsKey(categoryId))
                .toList();
        if (!missingRedisCategoryIds.isEmpty()) {
            for (CategoryStatsMapper.TopCategoryRow row : categoryStatsMapper.selectActiveCategoriesByIds(missingRedisCategoryIds)) {
                candidates.put(row.id(), Candidate.from(row));
            }
        }

        for (Map.Entry<String, Long> entry : redisDeltas.entrySet()) {
            Candidate candidate = candidates.get(entry.getKey());
            if (candidate != null) {
                candidate.addViews(entry.getValue());
            }
        }

        return candidates.values().stream()
                .sorted(Comparator.comparingLong(Candidate::viewCount).reversed()
                        .thenComparing(Candidate::name)
                        .thenComparing(Candidate::id))
                .limit(limit)
                .map(Candidate::toResponse)
                .toList();
    }

    private Map<String, Long> readRedisDeltas() {
        Map<String, Long> deltas = new LinkedHashMap<>();
        Set<String> categoryIds = new LinkedHashSet<>();
        for (String key : keyScanner.scanCumulativeViewKeys()) {
            if (CategoryStatsRedisKeys.isCumulativeViewKey(key)) {
                categoryIds.add(CategoryStatsRedisKeys.categoryIdFromCumulativeKey(key));
            }
        }
        for (String categoryId : categoryIds) {
            String key = CategoryStatsRedisKeys.cumulativeView(categoryId);
            parsePositiveLong(redisTemplate.opsForValue().get(key))
                    .ifPresent(delta -> deltas.put(categoryId, delta));
        }
        return deltas;
    }

    private List<TopCategoryResponse> readCachedTopCategories(String cacheKey) {
        String cachedPayload = redisTemplate.opsForValue().get(cacheKey);
        if (cachedPayload == null || cachedPayload.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(cachedPayload, TOP_CATEGORY_LIST_TYPE);
        } catch (JsonProcessingException exception) {
            log.warn("Invalid top category cache payload ignored for key={}", cacheKey);
            redisTemplate.delete(cacheKey);
            return null;
        }
    }

    private void cacheTopCategories(String cacheKey, List<TopCategoryResponse> response) {
        try {
            redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(response), TOP_CACHE_TTL);
        } catch (JsonProcessingException exception) {
            log.warn("Failed to serialize top category cache payload for key={}", cacheKey, exception);
        }
    }

    private String requireActiveCategoryId(String categoryId) {
        if (categoryId == null || categoryId.isBlank()) {
            throw new BusinessException("CATEGORY_NOT_FOUND", "分类不存在或不可用", HttpStatus.NOT_FOUND);
        }
        String normalizedCategoryId = categoryId.trim();
        CategoryEntity category = categoryMapper.selectById(normalizedCategoryId);
        if (category == null || !"ACTIVE".equals(category.getStatus())) {
            throw new BusinessException("CATEGORY_NOT_FOUND", "分类不存在或不可用", HttpStatus.NOT_FOUND);
        }
        return normalizedCategoryId;
    }

    private int normalizeLimit(Integer requestedLimit) {
        if (requestedLimit == null) {
            return DEFAULT_LIMIT;
        }
        return Math.min(Math.max(requestedLimit, 1), MAX_LIMIT);
    }

    private Optional<Long> parsePositiveLong(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return Optional.empty();
        }
        try {
            long value = Long.parseLong(rawValue);
            return value > 0 ? Optional.of(value) : Optional.empty();
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    private static final class Candidate {
        private final String id;
        private final String categoryCode;
        private final String code;
        private final String name;
        private final String imageUrl;
        private long viewCount;

        private Candidate(String id, String categoryCode, String code, String name, String imageUrl, long viewCount) {
            this.id = id;
            this.categoryCode = categoryCode;
            this.code = code;
            this.name = name;
            this.imageUrl = imageUrl;
            this.viewCount = viewCount;
        }

        static Candidate from(CategoryStatsMapper.TopCategoryRow row) {
            return new Candidate(row.id(), row.categoryCode(), row.code(), row.name(), row.imageUrl(), row.viewCount());
        }

        void addViews(long delta) {
            this.viewCount += delta;
        }

        String id() {
            return id;
        }

        String name() {
            return name;
        }

        long viewCount() {
            return viewCount;
        }

        TopCategoryResponse toResponse() {
            return new TopCategoryResponse(id, categoryCode, code, name, imageUrl, viewCount);
        }
    }
}
