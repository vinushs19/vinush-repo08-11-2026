package com.college.resourcefinder.dto.resource;

import jakarta.validation.constraints.Pattern;

public class ResourceStatusRequest {

    @Pattern(regexp = "^(ACTIVE|ARCHIVED|DELETED)$", message = "Status must be ACTIVE, ARCHIVED, or DELETED.")
    private String status;

    private Boolean isActive;

    public ResourceStatusRequest() {}

    public ResourceStatusRequest(String status, Boolean isActive) {
        this.status = status;
        this.isActive = isActive;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
}
