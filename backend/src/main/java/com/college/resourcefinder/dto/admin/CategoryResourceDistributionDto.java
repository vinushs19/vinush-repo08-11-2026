package com.college.resourcefinder.dto.admin;

/**
 * Distribution of academic resources across academic categories.
 */
public class CategoryResourceDistributionDto {

    private String categoryName;
    private long resourceCount;

    public CategoryResourceDistributionDto() {}

    public CategoryResourceDistributionDto(String categoryName, long resourceCount) {
        this.categoryName = categoryName;
        this.resourceCount = resourceCount;
    }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public long getResourceCount() { return resourceCount; }
    public void setResourceCount(long resourceCount) { this.resourceCount = resourceCount; }
}
