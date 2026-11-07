package com.college.resourcefinder.dto.filesave;

public class ApprovedCategoryResponse {

    private String code;
    private String displayName;
    private String targetDirectory;
    private String description;

    public ApprovedCategoryResponse() {}

    public ApprovedCategoryResponse(String code, String displayName, String targetDirectory, String description) {
        this.code = code;
        this.displayName = displayName;
        this.targetDirectory = targetDirectory;
        this.description = description;
    }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getTargetDirectory() { return targetDirectory; }
    public void setTargetDirectory(String targetDirectory) { this.targetDirectory = targetDirectory; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
