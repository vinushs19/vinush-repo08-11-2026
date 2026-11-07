package com.college.resourcefinder.dto.resource;

import com.college.resourcefinder.entity.Resource;

import java.time.LocalDateTime;

public class ResourceResponse {

    private Long resourceId;
    private String title;
    private String description;
    private String fileName;
    private String originalFilename;
    private String fileType;
    private Long fileSize;
    private String fileSizeFormatted;
    private String resourceType;

    // Academic Taxonomy
    private Long departmentId;
    private String departmentCode;
    private String departmentName;

    private Long subjectId;
    private String subjectCode;
    private String subjectName;
    private Integer semester;

    private Long unitId;
    private Integer unitNumber;
    private String unitTitle;

    private Long categoryId;
    private String categoryName;

    // Ownership & Metadata
    private Long uploadedById;
    private String uploadedByName;
    private String uploadedByRole;

    private String summary;
    private Boolean savedViaAi;
    private String status;
    private Boolean isActive;
    private LocalDateTime uploadDate;
    private LocalDateTime updatedAt;

    public ResourceResponse() {}

    public static ResourceResponse fromEntity(Resource resource) {
        if (resource == null) return null;

        ResourceResponse resp = new ResourceResponse();
        resp.setResourceId(resource.getResourceId());
        resp.setTitle(resource.getTitle());
        resp.setDescription(resource.getDescription());
        resp.setFileName(resource.getFileName());
        resp.setOriginalFilename(resource.getOriginalFilename());
        resp.setFileType(resource.getFileType());
        resp.setFileSize(resource.getFileSize());
        resp.setFileSizeFormatted(formatFileSize(resource.getFileSize()));
        resp.setResourceType(resource.getResourceType());

        if (resource.getSubject() != null) {
            resp.setSubjectId(resource.getSubject().getSubjectId());
            resp.setSubjectCode(resource.getSubject().getSubjectCode());
            resp.setSubjectName(resource.getSubject().getSubjectName());
            resp.setSemester(resource.getSubject().getSemester());

            if (resource.getSubject().getDepartment() != null) {
                resp.setDepartmentId(resource.getSubject().getDepartment().getDepartmentId());
                resp.setDepartmentCode(resource.getSubject().getDepartment().getDepartmentCode());
                resp.setDepartmentName(resource.getSubject().getDepartment().getDepartmentName());
            }
        }

        if (resource.getUnit() != null) {
            resp.setUnitId(resource.getUnit().getUnitId());
            resp.setUnitNumber(resource.getUnit().getUnitNumber());
            resp.setUnitTitle(resource.getUnit().getUnitTitle());
        }

        if (resource.getCategory() != null) {
            resp.setCategoryId(resource.getCategory().getCategoryId());
            resp.setCategoryName(resource.getCategory().getCategoryName());
        }

        if (resource.getUploadedBy() != null) {
            resp.setUploadedById(resource.getUploadedBy().getUserId());
            resp.setUploadedByName(resource.getUploadedBy().getName());
            resp.setUploadedByRole(resource.getUploadedBy().getRole());
        }

        resp.setSummary(resource.getSummary());
        resp.setSavedViaAi(resource.getSavedViaAi());
        resp.setStatus(resource.getStatus());
        resp.setIsActive(resource.getIsActive());
        resp.setUploadDate(resource.getUploadDate());
        resp.setUpdatedAt(resource.getUpdatedAt());

        return resp;
    }

    public static String formatFileSize(Long bytes) {
        if (bytes == null || bytes <= 0) return "0 B";
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));
        digitGroups = Math.min(digitGroups, units.length - 1);
        return String.format(java.util.Locale.US, "%.1f %s", bytes / Math.pow(1024, digitGroups), units[digitGroups]);
    }

    // Getters and Setters

    public Long getResourceId() { return resourceId; }
    public void setResourceId(Long resourceId) { this.resourceId = resourceId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getOriginalFilename() { return originalFilename; }
    public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }

    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public String getFileSizeFormatted() { return fileSizeFormatted; }
    public void setFileSizeFormatted(String fileSizeFormatted) { this.fileSizeFormatted = fileSizeFormatted; }

    public String getResourceType() { return resourceType; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }

    public Long getDepartmentId() { return departmentId; }
    public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }

    public String getDepartmentCode() { return departmentCode; }
    public void setDepartmentCode(String departmentCode) { this.departmentCode = departmentCode; }

    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }

    public Long getSubjectId() { return subjectId; }
    public void setSubjectId(Long subjectId) { this.subjectId = subjectId; }

    public String getSubjectCode() { return subjectCode; }
    public void setSubjectCode(String subjectCode) { this.subjectCode = subjectCode; }

    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }

    public Integer getSemester() { return semester; }
    public void setSemester(Integer semester) { this.semester = semester; }

    public Long getUnitId() { return unitId; }
    public void setUnitId(Long unitId) { this.unitId = unitId; }

    public Integer getUnitNumber() { return unitNumber; }
    public void setUnitNumber(Integer unitNumber) { this.unitNumber = unitNumber; }

    public String getUnitTitle() { return unitTitle; }
    public void setUnitTitle(String unitTitle) { this.unitTitle = unitTitle; }

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public Long getUploadedById() { return uploadedById; }
    public void setUploadedById(Long uploadedById) { this.uploadedById = uploadedById; }

    public String getUploadedByName() { return uploadedByName; }
    public void setUploadedByName(String uploadedByName) { this.uploadedByName = uploadedByName; }

    public String getUploadedByRole() { return uploadedByRole; }
    public void setUploadedByRole(String uploadedByRole) { this.uploadedByRole = uploadedByRole; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public Boolean getSavedViaAi() { return savedViaAi; }
    public void setSavedViaAi(Boolean savedViaAi) { this.savedViaAi = savedViaAi; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public LocalDateTime getUploadDate() { return uploadDate; }
    public void setUploadDate(LocalDateTime uploadDate) { this.uploadDate = uploadDate; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
