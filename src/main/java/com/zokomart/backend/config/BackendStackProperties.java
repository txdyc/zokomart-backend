package com.zokomart.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "zokomart.stack")
public record BackendStackProperties(
        String backendFramework,
        String persistenceFramework,
        String authFramework,
        String cache,
        String search
) {
}
