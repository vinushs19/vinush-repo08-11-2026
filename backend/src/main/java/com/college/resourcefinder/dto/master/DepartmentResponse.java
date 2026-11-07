package com.college.resourcefinder.dto.master;

import com.college.resourcefinder.entity.Department;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class DepartmentResponse {

    private Long departmentId;
    private String departmentCode;
    private String departmentName;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public DepartmentResponse() {}

    public static DepartmentResponse fromEntity(Department dept) {
        if (dept == null) return null;
        DepartmentResponse res = new DepartmentResponse();
        res.departmentId = dept.getDepartmentId();
        res.departmentCode = dept.getDepartmentCode();
        res.departmentName = dept.getDepartmentName();
        res.isActive = dept.getIsActive();
        res.createdAt = dept.getCreatedAt();
        res.updatedAt = dept.getUpdatedAt();
        return res;
    }

    public Long getDepartmentId() { return departmentId; }
    public String getDepartmentCode() { return departmentCode; }
    public String getDepartmentName() { return departmentName; }
    public Boolean getIsActive() { return isActive; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
