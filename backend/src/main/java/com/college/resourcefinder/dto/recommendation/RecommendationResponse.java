package com.college.resourcefinder.dto.recommendation;

import com.college.resourcefinder.dto.resource.ResourceResponse;

import java.util.ArrayList;
import java.util.List;

/**
 * Response DTO wrapping a recommended resource with its deterministic
 * relevance score and the list of criteria that contributed to its ranking.
 *
 * <p>The relevance score is computed using documented, weighted rules
 * (see {@link com.college.resourcefinder.service.RecommendationService})
 * and is fully deterministic for identical inputs.</p>
 */
public class RecommendationResponse {

    private ResourceResponse resource;
    private int relevanceScore;
    private List<String> matchedCriteria;

    public RecommendationResponse() {
        this.matchedCriteria = new ArrayList<>();
    }

    public RecommendationResponse(ResourceResponse resource, int relevanceScore, List<String> matchedCriteria) {
        this.resource = resource;
        this.relevanceScore = relevanceScore;
        this.matchedCriteria = matchedCriteria != null ? matchedCriteria : new ArrayList<>();
    }

    // ---- Getters and Setters ----

    public ResourceResponse getResource() { return resource; }
    public void setResource(ResourceResponse resource) { this.resource = resource; }

    public int getRelevanceScore() { return relevanceScore; }
    public void setRelevanceScore(int relevanceScore) { this.relevanceScore = relevanceScore; }

    public List<String> getMatchedCriteria() { return matchedCriteria; }
    public void setMatchedCriteria(List<String> matchedCriteria) { this.matchedCriteria = matchedCriteria; }
}
