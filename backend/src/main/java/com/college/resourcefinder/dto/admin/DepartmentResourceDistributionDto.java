package com.college.resourcefinder.dto.admin;

/**
 * Distribution of academic resources and storage consumption across college departments.
 */
public class DepartmentResourceDistributionDto {

    private String departmentCode;
    private String departmentName;
    private long resourceCount;
    private long storageBytes;
    private String storageFormatted;

    public DepartmentResourceDistributionDto() {}

    public DepartmentResourceDistributionDto(String departmentCode, String departmentName,
                                             long resourceCount, long storageBytes, String storageFormatted) {
        this.departmentCode = departmentCode;
        this.departmentName = departmentName;
        this.resourceCount = resourceCount;
        this.storageBytes = storageBytes;
        this.storageFormatted = storageFormatted;
    }

    public String getDepartmentCode() { return departmentCode; }
    public void setDepartmentCode(String departmentCode) { this.departmentCode = departmentCode; }

    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }

    public long getResourceCount() { return resourceCount; }
    public void setResourceCount(long resourceCount) { this.resourceCount = resourceCount; }

    public long getStorageBytes() { return storageBytes; }
    public void setStorageBytes(long storageBytes) { this.storageBytes = storageBytes; }

    public String getStorageFormatted() { return storageFormatted; }
    public void setStorageFormatted(String storageFormatted) { this.storageFormatted = storageFormatted; }
}
