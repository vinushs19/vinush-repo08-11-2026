package com.college.resourcefinder.service;

import com.college.resourcefinder.config.StorageProperties;
import com.college.resourcefinder.exception.StorageException;
import com.college.resourcefinder.util.FileValidator;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.UUID;

/**
 * Storage service managing local file storage for academic resources.
 * Prevents arbitrary filesystem traversal and ensures controlled, isolated storage.
 */
@Service
public class StorageService {

    private static final Logger log = LoggerFactory.getLogger(StorageService.class);

    private final Path rootLocation;
    private final FileValidator fileValidator;

    public StorageService(StorageProperties storageProperties, FileValidator fileValidator) {
        this.rootLocation = Paths.get(storageProperties.getUploadDir()).toAbsolutePath().normalize();
        this.fileValidator = fileValidator;
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(rootLocation);
            log.info("StorageService initialized at absolute root: {}", rootLocation);
        } catch (IOException e) {
            throw new StorageException("Could not initialize storage directory: " + rootLocation, e);
        }
    }

    /**
     * Stores an uploaded file within a partitioned sub-directory.
     *
     * @param file the uploaded multipart file
     * @param subDirectory optional partitioned path like "MCA/SEM1/CS8101"
     * @return metadata describing the saved file
     */
    public StoredFile storeFile(MultipartFile file, String subDirectory) {
        fileValidator.validate(file);

        String originalFilename = fileValidator.sanitize(file.getOriginalFilename());
        String extension = fileValidator.getExtension(originalFilename).toLowerCase();
        String uniqueFilename = "res_" + UUID.randomUUID().toString().replace("-", "") + "." + extension;

        Path targetDir = rootLocation;
        String normalizedSubDir = "";
        if (subDirectory != null && !subDirectory.isBlank()) {
            // Guard against traversal in subDirectory
            String cleanSubDir = subDirectory.replace('\\', '/').replaceAll("/+", "/");
            if (cleanSubDir.contains("..")) {
                throw new StorageException("Invalid sub-directory structure: traversal detected.");
            }
            targetDir = rootLocation.resolve(cleanSubDir).normalize();
            normalizedSubDir = cleanSubDir.endsWith("/") ? cleanSubDir : cleanSubDir + "/";
        }

        // Verify destination is still within root location
        if (!targetDir.startsWith(rootLocation)) {
            throw new SecurityException("Cannot store file outside current storage root directory.");
        }

        try {
            Files.createDirectories(targetDir);
            Path destinationPath = targetDir.resolve(uniqueFilename).normalize();

            if (!destinationPath.startsWith(rootLocation)) {
                throw new SecurityException("Cannot store file outside current storage root directory.");
            }

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, destinationPath, StandardCopyOption.REPLACE_EXISTING);
            }

            String relativeFilePath = (normalizedSubDir + uniqueFilename).replace('\\', '/');
            log.info("Successfully stored resource file: {} ({} bytes)", relativeFilePath, file.getSize());

            return new StoredFile(
                    uniqueFilename,
                    originalFilename,
                    relativeFilePath,
                    extension,
                    file.getSize()
            );
        } catch (IOException e) {
            throw new StorageException("Failed to store file " + originalFilename, e);
        }
    }

    /**
     * Loads a stored file as an org.springframework.core.io.Resource for streaming/download.
     * Prevents arbitrary path traversal and safely protects internal server filesystem paths.
     *
     * @param relativeFilePath the relative path under the storage root
     * @return Spring Resource
     */
    public Resource loadAsResource(String relativeFilePath) {
        if (relativeFilePath == null || relativeFilePath.isBlank()) {
            throw new StorageException("File path cannot be empty.");
        }

        try {
            Path file = rootLocation.resolve(relativeFilePath).normalize();

            // Strict traversal security verification
            if (!file.startsWith(rootLocation)) {
                log.warn("Path traversal attempt detected with relative path: {}", relativeFilePath);
                throw new SecurityException("Access to arbitrary file system path is strictly forbidden.");
            }

            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                log.warn("Physical file missing on server: {}", relativeFilePath);
                throw new com.college.resourcefinder.exception.ResourceNotFoundException(
                        "The requested physical file is not available on the server."
                );
            }
        } catch (MalformedURLException e) {
            throw new com.college.resourcefinder.exception.ResourceNotFoundException(
                    "The requested physical file is not available on the server."
            );
        }
    }

    /**
     * Checks if a file exists on disk within the storage root.
     */
    public boolean fileExists(String relativeFilePath) {
        if (relativeFilePath == null || relativeFilePath.isBlank()) {
            return false;
        }
        try {
            Path file = rootLocation.resolve(relativeFilePath).normalize();
            return file.startsWith(rootLocation) && Files.exists(file);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Safely deletes a file from the storage directory.
     *
     * @param relativeFilePath the relative path to remove
     * @return true if deleted, false if file did not exist
     */
    public boolean deleteFile(String relativeFilePath) {
        if (relativeFilePath == null || relativeFilePath.isBlank()) {
            return false;
        }
        try {
            Path file = rootLocation.resolve(relativeFilePath).normalize();
            if (!file.startsWith(rootLocation)) {
                throw new SecurityException("Access to arbitrary file system path is strictly forbidden.");
            }
            return Files.deleteIfExists(file);
        } catch (IOException e) {
            log.warn("Failed to delete physical file {}: {}", relativeFilePath, e.getMessage());
            return false;
        }
    }

    public Path getRootLocation() {
        return rootLocation;
    }

    /**
     * Copies and organizes an existing stored resource file to an allowlisted destination directory.
     * Prevents path traversal, enforces collision-free naming, and respects the overwrite flag.
     *
     * @param sourceRelativePath existing relative path of the source file
     * @param destinationSubDir allowlisted destination directory (e.g. "organized/lecture_notes/SEM2")
     * @param originalFilename original resource filename for sanitization
     * @param overwrite whether overwriting is permitted
     * @return StoredFile record describing the newly organized file
     */
    public StoredFile copyAndOrganizeFile(String sourceRelativePath, String destinationSubDir,
                                          String originalFilename, boolean overwrite) {
        if (sourceRelativePath == null || sourceRelativePath.isBlank()) {
            throw new StorageException("Source file path cannot be empty.");
        }

        Path sourcePath = rootLocation.resolve(sourceRelativePath).normalize();
        if (!sourcePath.startsWith(rootLocation)) {
            throw new SecurityException("Source file path is outside the storage root directory.");
        }
        if (!Files.exists(sourcePath) || !Files.isReadable(sourcePath)) {
            throw new com.college.resourcefinder.exception.ResourceNotFoundException(
                    "The source physical file is not available on the server: " + sourceRelativePath);
        }

        if (destinationSubDir == null || destinationSubDir.isBlank()) {
            throw new StorageException("Destination directory cannot be empty.");
        }

        // Strict traversal check on destination
        String cleanDestDir = destinationSubDir.replace('\\', '/').replaceAll("/+", "/");
        if (cleanDestDir.contains("..")) {
            throw new SecurityException("Path traversal attempt detected in destination path.");
        }

        Path targetDir = rootLocation.resolve(cleanDestDir).normalize();
        if (!targetDir.startsWith(rootLocation)) {
            throw new SecurityException("Cannot save file outside current storage root directory.");
        }

        try {
            Files.createDirectories(targetDir);

            String safeOriginal = (originalFilename != null && !originalFilename.isBlank()) ?
                    fileValidator.sanitize(originalFilename) : "resource_file";
            String extension = fileValidator.getExtension(safeOriginal).toLowerCase();

            // Collision-free unique filename
            String uniqueFilename = "saved_" + UUID.randomUUID().toString().replace("-", "") + "." + extension;
            Path destinationPath = targetDir.resolve(uniqueFilename).normalize();

            if (!destinationPath.startsWith(rootLocation)) {
                throw new SecurityException("Cannot save file outside current storage root directory.");
            }

            if (Files.exists(destinationPath) && !overwrite) {
                throw new StorageException("Destination file already exists and overwrite was not authorized.");
            }

            Files.copy(sourcePath, destinationPath, overwrite ? StandardCopyOption.REPLACE_EXISTING : StandardCopyOption.COPY_ATTRIBUTES);

            long copiedSize = Files.size(destinationPath);
            String normalizedSubDir = cleanDestDir.endsWith("/") ? cleanDestDir : cleanDestDir + "/";
            String relativeDestPath = (normalizedSubDir + uniqueFilename).replace('\\', '/');

            log.info("Successfully copied and organized file from {} to {} ({} bytes)",
                    sourceRelativePath, relativeDestPath, copiedSize);

            return new StoredFile(
                    uniqueFilename,
                    safeOriginal,
                    relativeDestPath,
                    extension,
                    copiedSize
            );
        } catch (IOException e) {
            throw new StorageException("Failed to copy and organize file to " + destinationSubDir, e);
        }
    }

    /**
     * Resolves and verifies an absolute path within the configured storage root, defending against path traversal.
     */
    public Path resolveAbsolutePath(String relativeFilePath) {
        if (relativeFilePath == null || relativeFilePath.isBlank()) {
            throw new StorageException("Relative file path cannot be empty.");
        }
        Path file = rootLocation.resolve(relativeFilePath).normalize();
        if (!file.startsWith(rootLocation)) {
            throw new SecurityException("Access to arbitrary file system path is strictly forbidden.");
        }
        return file;
    }

    /**
     * Stored file metadata result.
     */
    public record StoredFile(
            String uniqueFilename,
            String originalFilename,
            String relativeFilePath,
            String fileType,
            long fileSize
    ) {}
}
