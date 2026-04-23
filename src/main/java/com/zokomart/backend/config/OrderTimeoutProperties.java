package com.zokomart.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "zokomart.order")
public record OrderTimeoutProperties(
        long paymentTimeoutMinutes,
        Duration autoCancelScanInterval,
        int autoCancelBatchSize
) {
}
