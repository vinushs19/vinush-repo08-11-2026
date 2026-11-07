package com.college.resourcefinder.dto.admin;

import java.util.ArrayList;
import java.util.List;

/**
 * Summary of search activity, including total searches, unique queries, and top queries.
 */
public class SearchActivitySummaryDto {

    private long totalSearches;
    private long uniqueQueriesCount;
    private List<QueryFrequencyDto> topQueries = new ArrayList<>();

    public SearchActivitySummaryDto() {}

    public SearchActivitySummaryDto(long totalSearches, long uniqueQueriesCount, List<QueryFrequencyDto> topQueries) {
        this.totalSearches = totalSearches;
        this.uniqueQueriesCount = uniqueQueriesCount;
        this.topQueries = topQueries != null ? topQueries : new ArrayList<>();
    }

    public long getTotalSearches() { return totalSearches; }
    public void setTotalSearches(long totalSearches) { this.totalSearches = totalSearches; }

    public long getUniqueQueriesCount() { return uniqueQueriesCount; }
    public void setUniqueQueriesCount(long uniqueQueriesCount) { this.uniqueQueriesCount = uniqueQueriesCount; }

    public List<QueryFrequencyDto> getTopQueries() { return topQueries; }
    public void setTopQueries(List<QueryFrequencyDto> topQueries) { this.topQueries = topQueries; }
}
