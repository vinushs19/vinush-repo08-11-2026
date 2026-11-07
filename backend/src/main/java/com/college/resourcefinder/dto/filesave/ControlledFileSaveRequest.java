package com.college.resourcefinder.dto.filesave;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class ControlledFileSaveRequest {

    @NotNull(message = "Resource ID is required")
    private Long resourceId;

    @NotBlank(message = "Destination category is required")
    @Pattern(regexp = "^(LECTURE_NOTES|QUESTION_PAPERS|ASSIGNMENTS|LAB_MANUALS|STUDY_MATERIALS|SYLLABUS|ARCHIVES)$",
            message = "Destination category must be one of: LECTURE_NOTES, QUESTION_PAPERS, ASSIGNMENTS, LAB_MANUALS, STUDY_MATERIALS, SYLLABUS, ARCHIVES")
    private String destinationCategory;

    @Pattern(regexp = "^[a-zA-Z0-9_-]{1,50}$",
            message = "Optional subfolder tag must contain only alphanumeric characters, underscores, or hyphens (1-50 chars)")
    private String subFolderTag;

    private Boolean overwrite = false;

    @Size(max = 255, message = "Remarks must not exceed 255 characters")
    private String remarks;

    public ControlledFileSaveRequest() {}

    public ControlledFileSaveRequest(Long resourceId, String destinationCategory, String subFolderTag, Boolean overwrite, String remarks) {
        this.resourceId = resourceId;
        this.destinationCategory = destinationCategory;
        this.subFolderTag = subFolderTag;
        this.overwrite = overwrite != null ? overwrite : false;
        this.remarks = remarks;
    }

    public Long getResourceId() { return resourceId; }
    public void setResourceId(Long resourceId) { this.resourceId = resourceId; }

    public String getDestinationCategory() { return destinationCategory; }
    public void setDestinationCategory(String destinationCategory) { this.destinationCategory = destinationCategory; }

    public String getSubFolderTag() { return subFolderTag; }
    public void setSubFolderTag(String subFolderTag) { this.subFolderTag = subFolderTag; }

    public Boolean getOverwrite() { return overwrite; }
    public void setOverwrite(Boolean overwrite) { this.overwrite = overwrite != null ? overwrite : false; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
}
