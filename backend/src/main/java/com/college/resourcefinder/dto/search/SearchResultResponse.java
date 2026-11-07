package com.college.resourcefinder.dto.search;

import com.college.resourcefinder.dto.resource.ResourceResponse;
import org.springframework.data.domain.Page;

import java.util.List;

public class SearchResultResponse {

    private String rawQuery;
    private DetectedCriteriaDto detectedCriteria;
    private long totalResults;
    private int page;
    private int totalPages;
    private List<ResourceResponse> results;

    public SearchResultResponse() {}

    public SearchResultResponse(String rawQuery, DetectedCriteriaDto detectedCriteria, Page<ResourceResponse> pageData) {
        this.rawQuery = rawQuery;
        this.detectedCriteria = detectedCriteria;
        this.totalResults = pageData.getTotalElements();
        this.page = pageData.getNumber();
        this.totalPages = pageData.getTotalPages();
        this.results = pageData.getContent();
    }

    public String getRawQuery() { return rawQuery; }
    public void setRawQuery(String rawQuery) { this.rawQuery = rawQuery; }

    public DetectedCriteriaDto getDetectedCriteria() { return detectedCriteria; }
    public void setDetectedCriteria(DetectedCriteriaDto detectedCriteria) { this.detectedCriteria = detectedCriteria; }

    public long getTotalResults() { return totalResults; }
    public void setTotalResults(long totalResults) { this.totalResults = totalResults; }

    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }

    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }

    public List<ResourceResponse> getResults() { return results; }
    public void setResults(List<ResourceResponse> results) { this.results = results; }
}
