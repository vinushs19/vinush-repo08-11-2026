package com.college.resourcefinder.dto.attendance;

import java.util.ArrayList;
import java.util.List;

public class BatchImportResultResponse {

    private int totalSubmitted;
    private int successCount;
    private int duplicateCount;
    private int failureCount;
    private String origin;
    private List<String> errors;
    private List<StaffAttendanceResponse> records;

    public BatchImportResultResponse() {
        this.errors = new ArrayList<>();
        this.records = new ArrayList<>();
    }

    public BatchImportResultResponse(int totalSubmitted, int successCount, int duplicateCount,
                                     int failureCount, String origin, List<String> errors,
                                     List<StaffAttendanceResponse> records) {
        this.totalSubmitted = totalSubmitted;
        this.successCount = successCount;
        this.duplicateCount = duplicateCount;
        this.failureCount = failureCount;
        this.origin = origin;
        this.errors = errors != null ? errors : new ArrayList<>();
        this.records = records != null ? records : new ArrayList<>();
    }

    public int getTotalSubmitted() { return totalSubmitted; }
    public void setTotalSubmitted(int totalSubmitted) { this.totalSubmitted = totalSubmitted; }

    public int getSuccessCount() { return successCount; }
    public void setSuccessCount(int successCount) { this.successCount = successCount; }

    public int getDuplicateCount() { return duplicateCount; }
    public void setDuplicateCount(int duplicateCount) { this.duplicateCount = duplicateCount; }

    public int getFailureCount() { return failureCount; }
    public void setFailureCount(int failureCount) { this.failureCount = failureCount; }

    public String getOrigin() { return origin; }
    public void setOrigin(String origin) { this.origin = origin; }

    public List<String> getErrors() { return errors; }
    public void setErrors(List<String> errors) { this.errors = errors; }

    public List<StaffAttendanceResponse> getRecords() { return records; }
    public void setRecords(List<StaffAttendanceResponse> records) { this.records = records; }
}
