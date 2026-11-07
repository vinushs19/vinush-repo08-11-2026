package com.college.resourcefinder.dto.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AiQueryParseRequest {

    @NotBlank(message = "Search query must not be blank")
    @Size(max = 500, message = "Query length must not exceed 500 characters")
    private String query;

    private String department;

    public AiQueryParseRequest() {}

    public AiQueryParseRequest(String query, String department) {
        this.query = query;
        this.department = department;
    }

    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
}
