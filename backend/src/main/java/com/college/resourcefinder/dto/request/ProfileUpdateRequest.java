package com.college.resourcefinder.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Profile update request payload.
 * Allows updating user display name, department association, and optional password rotation.
 */
public class ProfileUpdateRequest {

    @NotBlank(message = "Name cannot be empty.")
    @Size(min = 2, max = 150, message = "Name must be between 2 and 150 characters.")
    private String name;

    private Long departmentId;

    private String currentPassword;

    @Size(min = 8, max = 100, message = "New password must be at least 8 characters long.")
    private String newPassword;

    public ProfileUpdateRequest() {}

    public ProfileUpdateRequest(String name, Long departmentId, String currentPassword, String newPassword) {
        this.name = name;
        this.departmentId = departmentId;
        this.currentPassword = currentPassword;
        this.newPassword = newPassword;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name != null ? name.trim() : null;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    public String getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
