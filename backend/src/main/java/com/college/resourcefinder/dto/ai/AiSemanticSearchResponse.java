package com.college.resourcefinder.dto.ai;

import com.college.resourcefinder.dto.resource.ResourceResponse;
import java.util.ArrayList;
import java.util.List;

public class AiSemanticSearchResponse {

    private String query;
    private String similarityMetric = "Cosine Similarity (Character 3-gram + Word TF-IDF Vectors)";
    private List<ResourceResponse> results = new ArrayList<>();
    private int totalMatches;

    public AiSemanticSearchResponse() {}

    public AiSemanticSearchResponse(String query, List<ResourceResponse> results) {
        this.query = query;
        this.results = results != null ? results : new ArrayList<>();
        this.totalMatches = this.results.size();
    }

    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }

    public String getSimilarityMetric() { return similarityMetric; }
    public void setSimilarityMetric(String similarityMetric) { this.similarityMetric = similarityMetric; }

    public List<ResourceResponse> getResults() { return results; }
    public void setResults(List<ResourceResponse> results) {
        this.results = results != null ? results : new ArrayList<>();
        this.totalMatches = this.results.size();
    }

    public int getTotalMatches() { return totalMatches; }
    public void setTotalMatches(int totalMatches) { this.totalMatches = totalMatches; }
}
