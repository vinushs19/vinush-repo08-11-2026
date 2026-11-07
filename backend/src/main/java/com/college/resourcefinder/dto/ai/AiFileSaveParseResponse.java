package com.college.resourcefinder.dto.ai;

public class AiFileSaveParseResponse {

    private boolean valid;
    private Long resourceId;
    private String destinationCategory;
    private String subfolderTag;
    private String explanation;
    private String error;

    public AiFileSaveParseResponse() {}

    public boolean isValid() { return valid; }
    public void setValid(boolean valid) { this.valid = valid; }

    public Long getResourceId() { return resourceId; }
    public void setResourceId(Long resourceId) { this.resourceId = resourceId; }

    public String getDestinationCategory() { return destinationCategory; }
    public void setDestinationCategory(String destinationCategory) { this.destinationCategory = destinationCategory; }

    public String getSubfolderTag() { return subfolderTag; }
    public void setSubfolderTag(String subfolderTag) { this.subfolderTag = subfolderTag; }

    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
}
