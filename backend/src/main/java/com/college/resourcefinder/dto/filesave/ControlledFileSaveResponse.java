package com.college.resourcefinder.dto.filesave;

import com.college.resourcefinder.dto.resource.ResourceResponse;
import com.college.resourcefinder.entity.FileOperation;
import java.time.LocalDateTime;

public class ControlledFileSaveResponse {

    private Long operationId;
    private Long resourceId;
    private String resourceTitle;
    private String sourceFilePath;
    private String destinationCategory;
    private String destinationPath;
    private String destinationFilename;
    private Long fileSize;
    private String fileSizeFormatted;
    private String fileType;
    private String operationType;
    private String status;
    private String errorMessage;
    private Long userId;
    private String userName;
    private LocalDateTime createdAt;

    public ControlledFileSaveResponse() {}

    public static ControlledFileSaveResponse fromEntity(FileOperation op) {
        if (op == null) return null;

        ControlledFileSaveResponse resp = new ControlledFileSaveResponse();
        resp.setOperationId(op.getOperationId());

        if (op.getResource() != null) {
            resp.setResourceId(op.getResource().getResourceId());
            resp.setResourceTitle(op.getResource().getTitle());
        }

        resp.setSourceFilePath(op.getSourceFilePath());
        resp.setDestinationCategory(op.getDestinationCategory());
        resp.setDestinationPath(op.getDestinationPath());
        resp.setDestinationFilename(op.getDestinationFilename());
        resp.setFileSize(op.getFileSize());
        resp.setFileSizeFormatted(ResourceResponse.formatFileSize(op.getFileSize()));
        resp.setFileType(op.getFileType());
        resp.setOperationType(op.getOperationType());
        resp.setStatus(op.getStatus());
        resp.setErrorMessage(op.getErrorMessage());

        if (op.getUser() != null) {
            resp.setUserId(op.getUser().getUserId());
            resp.setUserName(op.getUser().getName());
        }

        resp.setCreatedAt(op.getCreatedAt());
        return resp;
    }

    // Getters and Setters
    public Long getOperationId() { return operationId; }
    public void setOperationId(Long operationId) { this.operationId = operationId; }

    public Long getResourceId() { return resourceId; }
    public void setResourceId(Long resourceId) { this.resourceId = resourceId; }

    public String getResourceTitle() { return resourceTitle; }
    public void setResourceTitle(String resourceTitle) { this.resourceTitle = resourceTitle; }

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

    public String getFileSizeFormatted() { return fileSizeFormatted; }
    public void setFileSizeFormatted(String fileSizeFormatted) { this.fileSizeFormatted = fileSizeFormatted; }

    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }

    public String getOperationType() { return operationType; }
    public void setOperationType(String operationType) { this.operationType = operationType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
