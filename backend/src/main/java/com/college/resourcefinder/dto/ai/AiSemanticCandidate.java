package com.college.resourcefinder.dto.ai;

public class AiSemanticCandidate {

    private Long resourceId;
    private String title;
    private String description;
    private String summary;
    private String subjectName;
    private String categoryName;
    private Double semanticSimilarity;

    public AiSemanticCandidate() {}

    public AiSemanticCandidate(Long resourceId, String title, String description,
                               String summary, String subjectName, String categoryName) {
        this.resourceId = resourceId;
        this.title = title;
        this.description = description;
        this.summary = summary;
        this.subjectName = subjectName;
        this.categoryName = categoryName;
    }

    public Long getResourceId() { return resourceId; }
    public void setResourceId(Long resourceId) { this.resourceId = resourceId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public Double getSemanticSimilarity() { return semanticSimilarity; }
    public void setSemanticSimilarity(Double semanticSimilarity) { this.semanticSimilarity = semanticSimilarity; }
}
