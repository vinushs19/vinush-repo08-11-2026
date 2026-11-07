package com.college.resourcefinder.dto.master;

import com.college.resourcefinder.entity.Semester;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SemesterResponse {

    private Long semesterId;
    private Integer semesterNumber;
    private String semesterName;
    private String term;
    private Boolean isActive;
    private LocalDateTime createdAt;

    public SemesterResponse() {}

    public static SemesterResponse fromEntity(Semester sem) {
        if (sem == null) return null;
        SemesterResponse res = new SemesterResponse();
        res.semesterId = sem.getSemesterId();
        res.semesterNumber = sem.getSemesterNumber();
        res.semesterName = sem.getSemesterName();
        res.term = sem.getTerm();
        res.isActive = sem.getIsActive();
        res.createdAt = sem.getCreatedAt();
        return res;
    }

    public Long getSemesterId() { return semesterId; }
    public Integer getSemesterNumber() { return semesterNumber; }
    public String getSemesterName() { return semesterName; }
    public String getTerm() { return term; }
    public Boolean getIsActive() { return isActive; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
