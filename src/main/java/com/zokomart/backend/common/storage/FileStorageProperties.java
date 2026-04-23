package com.zokomart.backend.common.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

@ConfigurationProperties(prefix = "zokomart.storage")
public record FileStorageProperties(Path rootPath, String publicBasePath) {
}

