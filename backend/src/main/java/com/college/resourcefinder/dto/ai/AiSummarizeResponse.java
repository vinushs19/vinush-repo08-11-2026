package com.college.resourcefinder.dto.ai;

import java.time.LocalDateTime;

public class AiSummarizeResponse {

    private Long resourceId;
    private String summary;
    private int wordCount;
    private int originalWordCount;
    private double reductionRatio;
    private String fileHash;
    private String method;
    private LocalDateTime generatedAt;
    private boolean cached;

    public AiSummarizeResponse() {}

    public Long getResourceId() { return resourceId; }
    public void setResourceId(Long resourceId) { this.resourceId = resourceId; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public int getWordCount() { return wordCount; }
    public void setWordCount(int wordCount) { this.wordCount = wordCount; }

    public int getOriginalWordCount() { return originalWordCount; }
    public void setOriginalWordCount(int originalWordCount) { this.originalWordCount = originalWordCount; }

    public double getReductionRatio() { return reductionRatio; }
    public void setReductionRatio(double reductionRatio) { this.reductionRatio = reductionRatio; }

    public String getFileHash() { return fileHash; }
    public void setFileHash(String fileHash) { this.fileHash = fileHash; }

    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }

    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }

    public boolean isCached() { return cached; }
    public void setCached(boolean cached) { this.cached = cached; }
}
