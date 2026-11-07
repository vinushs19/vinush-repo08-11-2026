package com.college.resourcefinder.dto.search;

import com.college.resourcefinder.entity.SearchHistory;

import java.time.LocalDateTime;

public class SearchHistoryResponse {

    private Long searchId;
    private String searchQuery;
    private Integer resultCount;
    private LocalDateTime searchDate;

    public SearchHistoryResponse() {}

    public static SearchHistoryResponse fromEntity(SearchHistory sh) {
        if (sh == null) return null;
        SearchHistoryResponse resp = new SearchHistoryResponse();
        resp.setSearchId(sh.getSearchId());
        resp.setSearchQuery(sh.getSearchQuery());
        resp.setResultCount(sh.getResultCount());
        resp.setSearchDate(sh.getSearchDate());
        return resp;
    }

    public Long getSearchId() { return searchId; }
    public void setSearchId(Long searchId) { this.searchId = searchId; }

    public String getSearchQuery() { return searchQuery; }
    public void setSearchQuery(String searchQuery) { this.searchQuery = searchQuery; }

    public Integer getResultCount() { return resultCount; }
    public void setResultCount(Integer resultCount) { this.resultCount = resultCount; }

    public LocalDateTime getSearchDate() { return searchDate; }
    public void setSearchDate(LocalDateTime searchDate) { this.searchDate = searchDate; }
}
