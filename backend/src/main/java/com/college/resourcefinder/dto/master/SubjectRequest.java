package com.college.resourcefinder.dto.master;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class SubjectRequest {

    @NotBlank(message = "Subject code is required.")
    @Size(min = 2, max = 30, message = "Subject code must be between 2 and 30 characters.")
    private String subjectCode;

    @NotBlank(message = "Subject name is required.")
    @Size(min = 3, max = 200, message = "Subject name must be between 3 and 200 characters.")
    private String subjectName;

    @NotNull(message = "Semester is required.")
    @Min(value = 1, message = "Semester must be at least 1.")
    @Max(value = 8, message = "Semester cannot exceed 8.")
    private Integer semester;

    @NotNull(message = "Department ID is required.")
    private Long departmentId;

    private String description;

    private Boolean isActive = true;

    public SubjectRequest() {}

    public SubjectRequest(String subjectCode, String subjectName, Integer semester, Long departmentId, String description, Boolean isActive) {
        this.subjectCode = subjectCode;
        this.subjectName = subjectName;
        this.semester = semester;
        this.departmentId = departmentId;
        this.description = description;
        this.isActive = isActive != null ? isActive : true;
    }

    public String getSubjectCode() { return subjectCode; }
    public void setSubjectCode(String subjectCode) { this.subjectCode = subjectCode != null ? subjectCode.trim().toUpperCase() : null; }

    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName != null ? subjectName.trim() : null; }

    public Integer getSemester() { return semester; }
    public void setSemester(Integer semester) { this.semester = semester; }

    public Long getDepartmentId() { return departmentId; }
    public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
}
