package com.college.resourcefinder.dto.master;

import com.college.resourcefinder.entity.Subject;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SubjectResponse {

    private Long subjectId;
    private String subjectCode;
    private String subjectName;
    private Integer semester;
    private Long departmentId;
    private String departmentName;
    private String departmentCode;
    private String description;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<UnitResponse> units;

    public SubjectResponse() {}

    public static SubjectResponse fromEntity(Subject subject) {
        return fromEntity(subject, false);
    }

    public static SubjectResponse fromEntity(Subject subject, boolean includeUnits) {
        if (subject == null) return null;
        SubjectResponse res = new SubjectResponse();
        res.subjectId = subject.getSubjectId();
        res.subjectCode = subject.getSubjectCode();
        res.subjectName = subject.getSubjectName();
        res.semester = subject.getSemester();
        res.description = subject.getDescription();
        res.isActive = subject.getIsActive();
        res.createdAt = subject.getCreatedAt();
        res.updatedAt = subject.getUpdatedAt();

        if (subject.getDepartment() != null) {
            res.departmentId = subject.getDepartment().getDepartmentId();
            res.departmentName = subject.getDepartment().getDepartmentName();
            res.departmentCode = subject.getDepartment().getDepartmentCode();
        }

        if (includeUnits && subject.getUnits() != null) {
            res.units = subject.getUnits().stream()
                    .map(UnitResponse::fromEntity)
                    .toList();
        }

        return res;
    }

    public Long getSubjectId() { return subjectId; }
    public String getSubjectCode() { return subjectCode; }
    public String getSubjectName() { return subjectName; }
    public Integer getSemester() { return semester; }
    public Long getDepartmentId() { return departmentId; }
    public String getDepartmentName() { return departmentName; }
    public String getDepartmentCode() { return departmentCode; }
    public String getDescription() { return description; }
    public Boolean getIsActive() { return isActive; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public List<UnitResponse> getUnits() { return units; }
}
