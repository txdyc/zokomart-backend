package com.zokomart.backend.catalog.stats;

import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.Locale;

final class CategoryStatsRedisKeys {

    static final String CUMULATIVE_PREFIX = "category:view:";
    private static final String DAILY_PREFIX = "category:view:daily:";
    private static final String WEEKLY_PREFIX = "category:view:weekly:";
    private static final String TOP_PREFIX = "category:top:";

    private CategoryStatsRedisKeys() {
    }

    static String cumulativeView(String categoryId) {
        return CUMULATIVE_PREFIX + categoryId;
    }

    static String dailyView(LocalDate date, String categoryId) {
        return DAILY_PREFIX + date + ":" + categoryId;
    }

    static String weeklyView(LocalDate date, String categoryId) {
        WeekFields weekFields = WeekFields.ISO;
        int weekBasedYear = date.get(weekFields.weekBasedYear());
        int weekOfYear = date.get(weekFields.weekOfWeekBasedYear());
        return WEEKLY_PREFIX + String.format(Locale.ROOT, "%04d-W%02d", weekBasedYear, weekOfYear) + ":" + categoryId;
    }

    static String top(int limit) {
        return TOP_PREFIX + limit;
    }

    static boolean isCumulativeViewKey(String key) {
        if (key == null || !key.startsWith(CUMULATIVE_PREFIX)) {
            return false;
        }
        String suffix = key.substring(CUMULATIVE_PREFIX.length());
        return !suffix.isBlank() && !suffix.contains(":");
    }

    static String categoryIdFromCumulativeKey(String key) {
        return key.substring(CUMULATIVE_PREFIX.length());
    }
}
