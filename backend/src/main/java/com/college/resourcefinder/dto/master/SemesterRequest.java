package com.college.resourcefinder.dto.master;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class SemesterRequest {

    @NotNull(message = "Semester number is required.")
    @Min(value = 1, message = "Semester number must be at least 1.")
    @Max(value = 8, message = "Semester number cannot exceed 8.")
    private Integer semesterNumber;

    @NotBlank(message = "Semester name is required.")
    @Size(min = 3, max = 100, message = "Semester name must be between 3 and 100 characters.")
    private String semesterName;

    @NotBlank(message = "Term is required (ODD or EVEN).")
    @Pattern(regexp = "^(?i)(ODD|EVEN)$", message = "Term must be either 'ODD' or 'EVEN'.")
    private String term;

    private Boolean isActive = true;

    public SemesterRequest() {}

    public SemesterRequest(Integer semesterNumber, String semesterName, String term, Boolean isActive) {
        this.semesterNumber = semesterNumber;
        this.semesterName = semesterName;
        this.term = term != null ? term.trim().toUpperCase() : "ODD";
        this.isActive = isActive != null ? isActive : true;
    }

    public Integer getSemesterNumber() { return semesterNumber; }
    public void setSemesterNumber(Integer semesterNumber) { this.semesterNumber = semesterNumber; }

    public String getSemesterName() { return semesterName; }
    public void setSemesterName(String semesterName) { this.semesterName = semesterName != null ? semesterName.trim() : null; }

    public String getTerm() { return term; }
    public void setTerm(String term) { this.term = term != null ? term.trim().toUpperCase() : null; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
}
