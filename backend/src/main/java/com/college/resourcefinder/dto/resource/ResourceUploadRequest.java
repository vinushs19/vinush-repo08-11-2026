package com.college.resourcefinder.dto.resource;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ResourceUploadRequest {

    @NotBlank(message = "Title is required.")
    @Size(min = 3, max = 300, message = "Title must be between 3 and 300 characters.")
    private String title;

    @Size(max = 2000, message = "Description cannot exceed 2000 characters.")
    private String description;

    @NotNull(message = "Subject is required.")
    private Long subjectId;

    private Long unitId;

    @NotNull(message = "Category is required.")
    private Long categoryId;

    @NotBlank(message = "Resource type is required.")
    @Size(max = 100, message = "Resource type cannot exceed 100 characters.")
    private String resourceType;

    public ResourceUploadRequest() {}

    public ResourceUploadRequest(String title, String description, Long subjectId, Long unitId, Long categoryId, String resourceType) {
        this.title = title;
        this.description = description;
        this.subjectId = subjectId;
        this.unitId = unitId;
        this.categoryId = categoryId;
        this.resourceType = resourceType;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title != null ? title.trim() : null; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description != null ? description.trim() : null; }

    public Long getSubjectId() { return subjectId; }
    public void setSubjectId(Long subjectId) { this.subjectId = subjectId; }

    public Long getUnitId() { return unitId; }
    public void setUnitId(Long unitId) { this.unitId = unitId; }

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public String getResourceType() { return resourceType; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType != null ? resourceType.trim() : null; }
}
