package com.college.resourcefinder.dto.ai;

/**
 * Structured query parsing result produced by AI / NLP.
 */
public class AiQueryParseResponse {

    private String originalQuery;
    private String normalizedKeywords;
    private String subject;
    private Integer semester;
    private Integer unit;
    private String category;
    private String department;
    private boolean departmentScoped;
    private double confidence;
    private String explanation;

    public AiQueryParseResponse() {}

    public String getOriginalQuery() { return originalQuery; }
    public void setOriginalQuery(String originalQuery) { this.originalQuery = originalQuery; }

    public String getNormalizedKeywords() { return normalizedKeywords; }
    public void setNormalizedKeywords(String normalizedKeywords) { this.normalizedKeywords = normalizedKeywords; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public Integer getSemester() { return semester; }
    public void setSemester(Integer semester) { this.semester = semester; }

    public Integer getUnit() { return unit; }
    public void setUnit(Integer unit) { this.unit = unit; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public boolean isDepartmentScoped() { return departmentScoped; }
    public void setDepartmentScoped(boolean departmentScoped) { this.departmentScoped = departmentScoped; }

    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }

    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }
}
