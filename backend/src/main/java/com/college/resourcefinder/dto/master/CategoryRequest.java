package com.college.resourcefinder.dto.master;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CategoryRequest {

    @NotBlank(message = "Category name is required.")
    @Size(min = 2, max = 100, message = "Category name must be between 2 and 100 characters.")
    private String categoryName;

    @Size(max = 500, message = "Description cannot exceed 500 characters.")
    private String description;

    private Boolean isActive = true;

    public CategoryRequest() {}

    public CategoryRequest(String categoryName, String description, Boolean isActive) {
        this.categoryName = categoryName;
        this.description = description;
        this.isActive = isActive != null ? isActive : true;
    }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName != null ? categoryName.trim() : null; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
}
