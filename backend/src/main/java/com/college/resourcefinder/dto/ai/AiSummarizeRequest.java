package com.college.resourcefinder.dto.ai;

public class AiSummarizeRequest {

    private int maxSentences = 4;
    private String provider;

    public AiSummarizeRequest() {}

    public AiSummarizeRequest(int maxSentences, String provider) {
        this.maxSentences = maxSentences;
        this.provider = provider;
    }

    public int getMaxSentences() { return maxSentences; }
    public void setMaxSentences(int maxSentences) { this.maxSentences = maxSentences; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
}
