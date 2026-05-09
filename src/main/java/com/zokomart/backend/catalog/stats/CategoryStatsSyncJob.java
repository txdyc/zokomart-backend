package com.zokomart.backend.catalog.stats;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CategoryStatsSyncJob {

    private final CategoryStatsService categoryStatsService;

    public CategoryStatsSyncJob(CategoryStatsService categoryStatsService) {
        this.categoryStatsService = categoryStatsService;
    }

    @Scheduled(
            initialDelayString = "${zokomart.category-stats.sync-interval:PT5M}",
            fixedDelayString = "${zokomart.category-stats.sync-interval:PT5M}"
    )
    public void syncCategoryViewStats() {
        categoryStatsService.syncRedisDeltasToDatabase();
    }
}
