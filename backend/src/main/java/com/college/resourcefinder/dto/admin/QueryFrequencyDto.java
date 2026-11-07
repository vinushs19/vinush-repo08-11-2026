package com.college.resourcefinder.dto.admin;

/**
 * Data transfer object representing the frequency count of a specific search query.
 */
public class QueryFrequencyDto {

    private String query;
    private long count;

    public QueryFrequencyDto() {}

    public QueryFrequencyDto(String query, long count) {
        this.query = query;
        this.count = count;
    }

    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }

    public long getCount() { return count; }
    public void setCount(long count) { this.count = count; }
}
