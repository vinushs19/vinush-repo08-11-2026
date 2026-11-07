package com.college.resourcefinder.dto.master;

import com.college.resourcefinder.entity.Unit;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class UnitResponse {

    private Long unitId;
    private Long subjectId;
    private String subjectCode;
    private Integer unitNumber;
    private String unitTitle;
    private String description;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public UnitResponse() {}

    public static UnitResponse fromEntity(Unit unit) {
        if (unit == null) return null;
        UnitResponse res = new UnitResponse();
        res.unitId = unit.getUnitId();
        res.unitNumber = unit.getUnitNumber();
        res.unitTitle = unit.getUnitTitle();
        res.description = unit.getDescription();
        res.isActive = unit.getIsActive();
        res.createdAt = unit.getCreatedAt();
        res.updatedAt = unit.getUpdatedAt();

        if (unit.getSubject() != null) {
            res.subjectId = unit.getSubject().getSubjectId();
            res.subjectCode = unit.getSubject().getSubjectCode();
        }

        return res;
    }

    public Long getUnitId() { return unitId; }
    public Long getSubjectId() { return subjectId; }
    public String getSubjectCode() { return subjectCode; }
    public Integer getUnitNumber() { return unitNumber; }
    public String getUnitTitle() { return unitTitle; }
    public String getDescription() { return description; }
    public Boolean getIsActive() { return isActive; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
