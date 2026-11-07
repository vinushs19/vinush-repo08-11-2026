package com.college.resourcefinder.dto.master;

import com.college.resourcefinder.entity.Category;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class CategoryResponse {

    private Long categoryId;
    private String categoryName;
    private String description;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public CategoryResponse() {}

    public static CategoryResponse fromEntity(Category category) {
        if (category == null) return null;
        CategoryResponse res = new CategoryResponse();
        res.categoryId = category.getCategoryId();
        res.categoryName = category.getCategoryName();
        res.description = category.getDescription();
        res.isActive = category.getIsActive();
        res.createdAt = category.getCreatedAt();
        res.updatedAt = category.getUpdatedAt();
        return res;
    }

    public Long getCategoryId() { return categoryId; }
    public String getCategoryName() { return categoryName; }
    public String getDescription() { return description; }
    public Boolean getIsActive() { return isActive; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
