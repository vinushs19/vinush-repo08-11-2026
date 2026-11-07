package com.college.resourcefinder.dto.admin;

import com.college.resourcefinder.entity.AuditLog;
import java.time.LocalDateTime;

/**
 * Data transfer object representing an audit log entry for administrator review.
 * Never exposes passwords, raw tokens, or document content.
 */
public class AuditLogResponse {

    private Long logId;
    private Long userId;
    private String userEmail;
    private String userName;
    private String action;
    private String resourceType;
    private String resourceIdentifier;
    private String ipAddress;
    private String status;
    private String details;
    private LocalDateTime timestamp;

    public AuditLogResponse() {}

    public static AuditLogResponse fromEntity(AuditLog log) {
        if (log == null) return null;
        AuditLogResponse dto = new AuditLogResponse();
        dto.setLogId(log.getLogId());
        if (log.getUser() != null) {
            dto.setUserId(log.getUser().getUserId());
            dto.setUserEmail(log.getUser().getEmail());
            dto.setUserName(log.getUser().getName());
        }
        dto.setAction(log.getAction());
        dto.setResourceType(log.getResourceType());
        dto.setResourceIdentifier(log.getResourceIdentifier());
        dto.setIpAddress(log.getIpAddress());
        dto.setStatus(log.getStatus());
        dto.setDetails(log.getDetails());
        dto.setTimestamp(log.getTimestamp());
        return dto;
    }

    public Long getLogId() { return logId; }
    public void setLogId(Long logId) { this.logId = logId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getResourceType() { return resourceType; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }

    public String getResourceIdentifier() { return resourceIdentifier; }
    public void setResourceIdentifier(String resourceIdentifier) { this.resourceIdentifier = resourceIdentifier; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
