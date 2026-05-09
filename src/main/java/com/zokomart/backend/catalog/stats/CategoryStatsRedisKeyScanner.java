package com.zokomart.backend.catalog.stats;

import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class CategoryStatsRedisKeyScanner {

    private static final int SCAN_COUNT = 1000;

    private final StringRedisTemplate redisTemplate;

    public CategoryStatsRedisKeyScanner(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public List<String> scanCumulativeViewKeys() {
        List<String> keys = redisTemplate.execute((RedisCallback<List<String>>) this::scanKeys);
        return Optional.ofNullable(keys).orElseGet(List::of);
    }

    private List<String> scanKeys(RedisConnection connection) {
        List<String> keys = new ArrayList<>();
        ScanOptions scanOptions = ScanOptions.scanOptions()
                .match(CategoryStatsRedisKeys.CUMULATIVE_PREFIX + "*")
                .count(SCAN_COUNT)
                .build();
        try (Cursor<byte[]> cursor = connection.keyCommands().scan(scanOptions)) {
            while (cursor.hasNext()) {
                String key = new String(cursor.next(), StandardCharsets.UTF_8);
                if (CategoryStatsRedisKeys.isCumulativeViewKey(key)) {
                    keys.add(key);
                }
            }
        }
        return keys;
    }
}
