package com.college.resourcefinder.dto.master;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class DepartmentRequest {

    @NotBlank(message = "Department code is required.")
    @Size(min = 2, max = 20, message = "Department code must be between 2 and 20 characters.")
    private String departmentCode;

    @NotBlank(message = "Department name is required.")
    @Size(min = 3, max = 150, message = "Department name must be between 3 and 150 characters.")
    private String departmentName;

    private Boolean isActive = true;

    public DepartmentRequest() {}

    public DepartmentRequest(String departmentCode, String departmentName, Boolean isActive) {
        this.departmentCode = departmentCode;
        this.departmentName = departmentName;
        this.isActive = isActive != null ? isActive : true;
    }

    public String getDepartmentCode() {
        return departmentCode;
    }

    public void setDepartmentCode(String departmentCode) {
        this.departmentCode = departmentCode != null ? departmentCode.trim().toUpperCase() : null;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName != null ? departmentName.trim() : null;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}
