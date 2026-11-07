package com.college.resourcefinder.dto.ai;

import com.college.resourcefinder.dto.resource.ResourceResponse;
import java.util.List;
import java.util.Map;

public class AiChatResponse {

    private String reply;
    private String actionType; // "NONE", "SEARCH_RESOURCES", "SUMMARIZE_RESOURCE", "ORGANIZE_FILE", "SECURITY_REJECTION", etc.
    private Map<String, Object> actionData;
    private List<ResourceResponse> resourceResults;
    private String sessionId;
    private long latencyMs;

    public AiChatResponse() {}

    public String getReply() { return reply; }
    public void setReply(String reply) { this.reply = reply; }

    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }

    public Map<String, Object> getActionData() { return actionData; }
    public void setActionData(Map<String, Object> actionData) { this.actionData = actionData; }

    public List<ResourceResponse> getResourceResults() { return resourceResults; }
    public void setResourceResults(List<ResourceResponse> resourceResults) { this.resourceResults = resourceResults; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public long getLatencyMs() { return latencyMs; }
    public void setLatencyMs(long latencyMs) { this.latencyMs = latencyMs; }
}
