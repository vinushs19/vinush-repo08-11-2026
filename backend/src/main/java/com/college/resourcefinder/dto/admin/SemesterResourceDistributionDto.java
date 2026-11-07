package com.college.resourcefinder.dto.admin;

/**
 * Distribution of academic resources across semester levels (1 through 8).
 */
public class SemesterResourceDistributionDto {

    private Integer semesterNumber;
    private long resourceCount;

    public SemesterResourceDistributionDto() {}

    public SemesterResourceDistributionDto(Integer semesterNumber, long resourceCount) {
        this.semesterNumber = semesterNumber;
        this.resourceCount = resourceCount;
    }

    public Integer getSemesterNumber() { return semesterNumber; }
    public void setSemesterNumber(Integer semesterNumber) { this.semesterNumber = semesterNumber; }

    public long getResourceCount() { return resourceCount; }
    public void setResourceCount(long resourceCount) { this.resourceCount = resourceCount; }
}
