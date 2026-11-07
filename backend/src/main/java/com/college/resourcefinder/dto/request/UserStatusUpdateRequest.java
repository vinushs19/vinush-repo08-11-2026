package com.college.resourcefinder.dto.request;

/**
 * Administrative user status and role update request.
 * Used exclusively by administrators to manage account status (active/deactivated) and role assignments.
 */
public class UserStatusUpdateRequest {

    private Boolean isActive;
    private String role;

    public UserStatusUpdateRequest() {}

    public UserStatusUpdateRequest(Boolean isActive, String role) {
        this.isActive = isActive;
        this.role = role;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
