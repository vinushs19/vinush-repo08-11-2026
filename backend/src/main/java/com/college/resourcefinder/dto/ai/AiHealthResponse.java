package com.college.resourcefinder.dto.ai;

public class AiHealthResponse {

    private String status; // "UP", "DEGRADED", "DOWN"
    private String activeProvider; // "FASTAPI", "LOCAL_JAVA_FALLBACK"
    private String serviceUrl;
    private long latencyMs;
    private boolean fallbackActive;
    private String details;

    public AiHealthResponse() {}

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getActiveProvider() { return activeProvider; }
    public void setActiveProvider(String activeProvider) { this.activeProvider = activeProvider; }

    public String getServiceUrl() { return serviceUrl; }
    public void setServiceUrl(String serviceUrl) { this.serviceUrl = serviceUrl; }

    public long getLatencyMs() { return latencyMs; }
    public void setLatencyMs(long latencyMs) { this.latencyMs = latencyMs; }

    public boolean isFallbackActive() { return fallbackActive; }
    public void setFallbackActive(boolean fallbackActive) { this.fallbackActive = fallbackActive; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
}
