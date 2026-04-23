package com.zokomart.backend.common.storage;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class LocalFileStorageService implements StorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("png", "jpg", "jpeg", "webp", "gif");

    private final FileStorageProperties properties;

    public LocalFileStorageService(FileStorageProperties properties) {
        this.properties = properties;
    }

    @Override
    public StoredObjectResult store(StorageObjectType type, String originalFilename, String contentType, byte[] bytes) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        String extension = resolveExtension(originalFilename, contentType);
        String relativePath = "%s/%d/%02d/%s.%s".formatted(
                type.directory(),
                today.getYear(),
                today.getMonthValue(),
                UUID.randomUUID(),
                extension
        );

        Path absolutePath = resolveUnderRoot(relativePath);
        try {
            Files.createDirectories(absolutePath.getParent());
            Files.write(absolutePath, bytes, StandardOpenOption.CREATE_NEW);
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to store file to local storage", exception);
        }

        String normalizedKey = relativePath.replace('\\', '/');
        String publicBasePath = normalizeBasePath(properties.publicBasePath());
        return new StoredObjectResult(
                normalizedKey,
                publicBasePath + "/" + normalizedKey,
                contentType,
                bytes.length,
                originalFilename
        );
    }

    @Override
    public void delete(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            return;
        }
        if (looksLikeDirectory(storageKey)) {
            return;
        }
        try {
            Files.deleteIfExists(resolveUnderRoot(storageKey));
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to delete stored file", exception);
        }
    }

    private String resolveExtension(String originalFilename, String contentType) {
        String candidate = null;
        if (originalFilename != null) {
            String safeName = originalFilename.replace('\\', '/');
            int lastSlash = safeName.lastIndexOf('/');
            if (lastSlash > -1 && lastSlash < safeName.length() - 1) {
                safeName = safeName.substring(lastSlash + 1);
            }
            int dot = safeName.lastIndexOf('.');
            if (dot > -1 && dot < safeName.length() - 1) {
                candidate = safeName.substring(dot + 1).toLowerCase(Locale.ROOT);
            }
        }

        if (candidate == null || !isAllowedExtension(candidate)) {
            candidate = extensionFromContentType(contentType);
        }
        if (candidate == null || !isAllowedExtension(candidate)) {
            return "bin";
        }
        return candidate;
    }

    private String extensionFromContentType(String contentType) {
        if (contentType == null) {
            return null;
        }
        return switch (contentType.toLowerCase(Locale.ROOT)) {
            case "image/png" -> "png";
            case "image/jpeg", "image/jpg" -> "jpg";
            case "image/webp" -> "webp";
            case "image/gif" -> "gif";
            default -> null;
        };
    }

    private boolean isAllowedExtension(String extension) {
        return extension != null && ALLOWED_EXTENSIONS.contains(extension);
    }

    private boolean looksLikeDirectory(String storageKey) {
        String normalized = storageKey.replace('\\', '/');
        return normalized.endsWith("/");
    }

    private String normalizeBasePath(String basePath) {
        if (basePath == null || basePath.isBlank()) {
            return "";
        }
        if (basePath.endsWith("/")) {
            return basePath.substring(0, basePath.length() - 1);
        }
        return basePath;
    }

    private Path resolveUnderRoot(String storageKey) {
        Path root = properties.rootPath().toAbsolutePath().normalize();
        Path candidate = root.resolve(storageKey).normalize();
        if (!candidate.startsWith(root)) {
            throw new IllegalArgumentException("Storage key resolves outside of rootPath");
        }
        return candidate;
    }
}
