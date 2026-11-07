package com.college.resourcefinder.dto.search;

import java.util.List;

/**
 * Encapsulates deterministic criteria extracted from natural search queries.
 */
public class DetectedCriteriaDto {

    private String originalQuery;
    private String normalizedKeywords;
    private Integer extractedSemester;
    private Integer extractedUnitNumber;
    private String extractedCategory;
    private String extractedResourceType;
    private String extractedSubjectToken;
    private boolean departmentScopeApplied;
    private List<String> extractedTokens;

    public DetectedCriteriaDto() {}

    public String getOriginalQuery() { return originalQuery; }
    public void setOriginalQuery(String originalQuery) { this.originalQuery = originalQuery; }

    public String getNormalizedKeywords() { return normalizedKeywords; }
    public void setNormalizedKeywords(String normalizedKeywords) { this.normalizedKeywords = normalizedKeywords; }

    public Integer getExtractedSemester() { return extractedSemester; }
    public void setExtractedSemester(Integer extractedSemester) { this.extractedSemester = extractedSemester; }

    public Integer getExtractedUnitNumber() { return extractedUnitNumber; }
    public void setExtractedUnitNumber(Integer extractedUnitNumber) { this.extractedUnitNumber = extractedUnitNumber; }

    public String getExtractedCategory() { return extractedCategory; }
    public void setExtractedCategory(String extractedCategory) { this.extractedCategory = extractedCategory; }

    public String getExtractedResourceType() { return extractedResourceType; }
    public void setExtractedResourceType(String extractedResourceType) { this.extractedResourceType = extractedResourceType; }

    public String getExtractedSubjectToken() { return extractedSubjectToken; }
    public void setExtractedSubjectToken(String extractedSubjectToken) { this.extractedSubjectToken = extractedSubjectToken; }

    public boolean isDepartmentScopeApplied() { return departmentScopeApplied; }
    public void setDepartmentScopeApplied(boolean departmentScopeApplied) { this.departmentScopeApplied = departmentScopeApplied; }

    public List<String> getExtractedTokens() { return extractedTokens; }
    public void setExtractedTokens(List<String> extractedTokens) { this.extractedTokens = extractedTokens; }
}
