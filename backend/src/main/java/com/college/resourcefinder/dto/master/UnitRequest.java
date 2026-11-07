package com.college.resourcefinder.dto.master;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class UnitRequest {

    @NotNull(message = "Unit number is required.")
    @Min(value = 1, message = "Unit number must be at least 1.")
    @Max(value = 10, message = "Unit number cannot exceed 10.")
    private Integer unitNumber;

    @NotBlank(message = "Unit title is required.")
    @Size(min = 3, max = 200, message = "Unit title must be between 3 and 200 characters.")
    private String unitTitle;

    private String description;

    private Boolean isActive = true;

    public UnitRequest() {}

    public UnitRequest(Integer unitNumber, String unitTitle, String description, Boolean isActive) {
        this.unitNumber = unitNumber;
        this.unitTitle = unitTitle;
        this.description = description;
        this.isActive = isActive != null ? isActive : true;
    }

    public Integer getUnitNumber() { return unitNumber; }
    public void setUnitNumber(Integer unitNumber) { this.unitNumber = unitNumber; }

    public String getUnitTitle() { return unitTitle; }
    public void setUnitTitle(String unitTitle) { this.unitTitle = unitTitle != null ? unitTitle.trim() : null; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
}
