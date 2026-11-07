package com.college.resourcefinder.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * JPA entity recording controlled file organization and saving transactions.
 * Mapped to the {@code file_operations} table.
 *
 * <p>Persists operation provenance: source file, destination category,
 * final relative storage path, file size, requesting user, and execution status.</p>
 */
@Entity
@Table(name = "file_operations")
public class FileOperation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "operation_id")
    private Long operationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resource_id", nullable = false)
    private Resource resource;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "source_file_path", nullable = false, length = 1000)
    private String sourceFilePath;

    @Column(name = "destination_category", nullable = false, length = 50)
    private String destinationCategory;

    @Column(name = "destination_path", nullable = false, length = 1000)
    private String destinationPath;

    @Column(name = "destination_filename", nullable = false, length = 500)
    private String destinationFilename;

    @Column(name = "file_size", nullable = false)
    private Long fileSize = 0L;

    @Column(name = "file_type", nullable = false, length = 50)
    private String fileType;

    @Column(name = "operation_type", nullable = false, length = 50)
    private String operationType = "COPY_ORGANIZE";

    @Column(name = "status", nullable = false, length = 20)
    private String status = "SUCCESS";

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = "SUCCESS";
        }
        if (this.operationType == null) {
            this.operationType = "COPY_ORGANIZE";
        }
    }

    // ---- Constructors ----

    public FileOperation() {}

    public FileOperation(Resource resource, User user, String sourceFilePath,
                         String destinationCategory, String destinationPath,
                         String destinationFilename, Long fileSize, String fileType,
                         String operationType, String status, String errorMessage) {
        this.resource = resource;
        this.user = user;
        this.sourceFilePath = sourceFilePath;
        this.destinationCategory = destinationCategory;
        this.destinationPath = destinationPath;
        this.destinationFilename = destinationFilename;
        this.fileSize = fileSize != null ? fileSize : 0L;
        this.fileType = fileType;
        this.operationType = operationType != null ? operationType : "COPY_ORGANIZE";
        this.status = status != null ? status : "SUCCESS";
        this.errorMessage = errorMessage;
    }

    // ---- Getters and Setters ----

    public Long getOperationId() { return operationId; }
    public void setOperationId(Long operationId) { this.operationId = operationId; }

    public Resource getResource() { return resource; }
    public void setResource(Resource resource) { this.resource = resource; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getSourceFilePath() { return sourceFilePath; }
    public void setSourceFilePath(String sourceFilePath) { this.sourceFilePath = sourceFilePath; }

    public String getDestinationCategory() { return destinationCategory; }
    public void setDestinationCategory(String destinationCategory) { this.destinationCategory = destinationCategory; }

    public String getDestinationPath() { return destinationPath; }
    public void setDestinationPath(String destinationPath) { this.destinationPath = destinationPath; }

    public String getDestinationFilename() { return destinationFilename; }
    public void setDestinationFilename(String destinationFilename) { this.destinationFilename = destinationFilename; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }

    public String getOperationType() { return operationType; }
    public void setOperationType(String operationType) { this.operationType = operationType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    // ---- equals / hashCode ----

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileOperation that = (FileOperation) o;
        return Objects.equals(operationId, that.operationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(operationId);
    }

    @Override
    public String toString() {
        return "FileOperation{id=" + operationId + ", dest='" + destinationPath +
               "', status='" + status + "'}";
    }
}
