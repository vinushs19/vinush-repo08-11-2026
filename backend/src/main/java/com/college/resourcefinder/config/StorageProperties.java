package com.college.resourcefinder.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Externalised configuration for file storage behaviour.
 * Bound to properties prefixed with {@code app.storage.*}
 */
@Configuration
@ConfigurationProperties(prefix = "app.storage")
public class StorageProperties {

    /** Root directory for uploaded file storage (relative or absolute). */
    private String uploadDir = "./storage";

    /** Comma-separated list of allowed file extensions (lowercase, no dots). */
    private String allowedExtensions = "pdf,docx,pptx,txt,xlsx,csv";

    /** Maximum allowed file size in megabytes. */
    private int maxFileSizeMb = 50;

    // ---- Getters and Setters ----

    public String getUploadDir() {
        return uploadDir;
    }

    public void setUploadDir(String uploadDir) {
        this.uploadDir = uploadDir;
    }

    public String getAllowedExtensions() {
        return allowedExtensions;
    }

    public void setAllowedExtensions(String allowedExtensions) {
        this.allowedExtensions = allowedExtensions;
    }

    public int getMaxFileSizeMb() {
        return maxFileSizeMb;
    }

    public void setMaxFileSizeMb(int maxFileSizeMb) {
        this.maxFileSizeMb = maxFileSizeMb;
    }
}
