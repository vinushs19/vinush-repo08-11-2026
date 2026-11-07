package com.college.resourcefinder.util;

import com.college.resourcefinder.config.StorageProperties;
import com.college.resourcefinder.exception.StorageException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

/**
 * Defensive utility class for validating uploaded files.
 *
 * <p>Enforces multi-layered security validation:</p>
 * <ol>
 *   <li>File must not be null or empty.</li>
 *   <li>File size must not exceed the configured maximum.</li>
 *   <li>Original filename must be clean and free of path traversal sequences.</li>
 *   <li>File extension must exist in the permitted allowlist.</li>
 *   <li>Declared MIME type must be consistent with the file format.</li>
 *   <li>Binary file signatures (magic bytes) must verify real file content.</li>
 * </ol>
 */
@Component
public class FileValidator {

    private final StorageProperties storageProperties;

    public FileValidator(StorageProperties storageProperties) {
        this.storageProperties = storageProperties;
    }

    /**
     * Validates the given multipart file against storage and security policy.
     *
     * @param file the uploaded file to validate
     * @throws StorageException if any validation rule is violated
     */
    public void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new StorageException("Uploaded file is empty or missing.");
        }

        long maxBytes = (long) storageProperties.getMaxFileSizeMb() * 1024 * 1024;
        if (file.getSize() > maxBytes) {
            throw new StorageException(
                "File size " + (file.getSize() / (1024 * 1024)) + " MB " +
                "exceeds the maximum allowed size of " + storageProperties.getMaxFileSizeMb() + " MB."
            );
        }

        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.isBlank()) {
            throw new StorageException("File has no name.");
        }

        // Path traversal guard
        if (originalName.contains("..") || originalName.contains("/") || originalName.contains("\\")) {
            throw new StorageException("Invalid file name: path traversal characters are not allowed.");
        }

        String extension = getExtension(originalName).toLowerCase();
        if (extension.isBlank()) {
            throw new StorageException("File must have a valid extension.");
        }

        List<String> allowed = Arrays.asList(
            storageProperties.getAllowedExtensions().toLowerCase().split(",")
        );
        if (!allowed.contains(extension)) {
            throw new StorageException(
                "File type '." + extension + "' is not permitted. " +
                "Allowed types: " + storageProperties.getAllowedExtensions()
            );
        }

        // MIME type consistency check
        validateMimeType(file, extension);

        // Binary file signature (magic bytes) validation
        validateFileSignature(file, extension);
    }

    /**
     * Validates that the declared client MIME type does not conflict with the extension.
     */
    private void validateMimeType(MultipartFile file, String extension) {
        String clientMime = file.getContentType();
        if (clientMime != null && !clientMime.isBlank() && !"application/octet-stream".equalsIgnoreCase(clientMime)) {
            String lowerMime = clientMime.toLowerCase();
            switch (extension.toLowerCase()) {
                case "pdf" -> {
                    if (!lowerMime.contains("pdf")) {
                        throw new StorageException("MIME type mismatch: declared MIME '" + clientMime + "' does not match extension '.pdf'");
                    }
                }
                case "docx" -> {
                    if (!lowerMime.contains("word") && !lowerMime.contains("officedocument") && !lowerMime.contains("zip")) {
                        throw new StorageException("MIME type mismatch: declared MIME '" + clientMime + "' does not match extension '.docx'");
                    }
                }
                case "pptx" -> {
                    if (!lowerMime.contains("presentation") && !lowerMime.contains("officedocument") && !lowerMime.contains("zip")) {
                        throw new StorageException("MIME type mismatch: declared MIME '" + clientMime + "' does not match extension '.pptx'");
                    }
                }
                case "xlsx" -> {
                    if (!lowerMime.contains("sheet") && !lowerMime.contains("officedocument") && !lowerMime.contains("zip")) {
                        throw new StorageException("MIME type mismatch: declared MIME '" + clientMime + "' does not match extension '.xlsx'");
                    }
                }
            }
        }
    }

    /**
     * Validates binary file signatures (magic numbers) to prevent extension spoofing.
     */
    public void validateFileSignature(MultipartFile file, String extension) {
        try (InputStream is = file.getInputStream()) {
            byte[] header = new byte[8];
            int bytesRead = is.read(header);
            if (bytesRead < 4) {
                throw new StorageException("Corrupted file: file size is too small to contain a valid header.");
            }

            switch (extension.toLowerCase()) {
                case "pdf" -> {
                    // %PDF header is 0x25, 0x50, 0x44, 0x46
                    if (header[0] != 0x25 || header[1] != 0x50 || header[2] != 0x44 || header[3] != 0x46) {
                        throw new StorageException("File signature verification failed: file content is not a valid PDF document.");
                    }
                }
                case "docx", "pptx", "xlsx" -> {
                    // Office Open XML (ZIP archive) header is 0x50, 0x4B, 0x03, 0x04 (PK\x03\x04)
                    if (header[0] != 0x50 || header[1] != 0x4B || header[2] != 0x03 || header[3] != 0x04) {
                        throw new StorageException("File signature verification failed: file content is not a valid Office Open XML document (" + extension.toUpperCase() + ").");
                    }
                }
                case "txt", "csv" -> {
                    for (int i = 0; i < bytesRead; i++) {
                        if (header[i] == 0x00) {
                            throw new StorageException("File signature verification failed: binary null characters detected in text file.");
                        }
                    }
                }
                default -> {
                    // Basic sanity check passed
                }
            }
        } catch (IOException e) {
            throw new StorageException("Failed to inspect file signature: " + e.getMessage(), e);
        }
    }

    /**
     * Extracts the lowercase file extension from a filename.
     * Returns an empty string if no extension is present.
     */
    public String getExtension(String filename) {
        if (filename == null) return "";
        int dotIndex = filename.lastIndexOf('.');
        return (dotIndex > 0) ? filename.substring(dotIndex + 1) : "";
    }

    /**
     * Sanitizes a filename by removing characters unsafe for filesystem use.
     * Replaces spaces and special characters with underscores.
     */
    public String sanitize(String filename) {
        if (filename == null) return "file";
        return filename.replaceAll("[^a-zA-Z0-9.\\-_]", "_");
    }
}
