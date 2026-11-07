package com.college.resourcefinder.dto.ai;

import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;

public class AiSemanticSearchRequest {

    @NotBlank(message = "Query must not be blank")
    private String query;

    private Long departmentId;
    private Long subjectId;
    private Integer semester;
    private Long categoryId;
    private int limit = 10;

    public AiSemanticSearchRequest() {}

    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }

    public Long getDepartmentId() { return departmentId; }
    public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }

    public Long getSubjectId() { return subjectId; }
    public void setSubjectId(Long subjectId) { this.subjectId = subjectId; }

    public Integer getSemester() { return semester; }
    public void setSemester(Integer semester) { this.semester = semester; }

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public int getLimit() { return limit; }
    public void setLimit(int limit) { this.limit = limit; }
}
