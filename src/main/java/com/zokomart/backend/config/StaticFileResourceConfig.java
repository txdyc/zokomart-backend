package com.zokomart.backend.config;

import com.zokomart.backend.common.storage.FileStorageProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableConfigurationProperties(FileStorageProperties.class)
public class StaticFileResourceConfig implements WebMvcConfigurer {

    private final FileStorageProperties properties;

    public StaticFileResourceConfig(FileStorageProperties properties) {
        this.properties = properties;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String publicBasePath = properties.publicBasePath();
        if (publicBasePath == null || publicBasePath.isBlank()) {
            return;
        }
        String resourcePattern = publicBasePath.endsWith("/") ? publicBasePath + "**" : publicBasePath + "/**";
        String resourceLocation = ensureTrailingSlash(properties.rootPath().toAbsolutePath().toUri().toString());
        registry.addResourceHandler(resourcePattern).addResourceLocations(resourceLocation);
    }

    private String ensureTrailingSlash(String location) {
        if (location.endsWith("/")) {
            return location;
        }
        return location + "/";
    }
}

