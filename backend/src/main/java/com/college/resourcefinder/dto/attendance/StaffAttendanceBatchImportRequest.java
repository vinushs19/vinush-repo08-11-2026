package com.college.resourcefinder.dto.attendance;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import java.util.List;

public class StaffAttendanceBatchImportRequest {

    @Pattern(regexp = "^(MANUAL_ENTRY|CSV_IMPORT|BIOMETRIC_SYNC)$",
            message = "Default data source must be one of: MANUAL_ENTRY, CSV_IMPORT, BIOMETRIC_SYNC")
    private String defaultDataSource = "CSV_IMPORT";

    @NotEmpty(message = "Attendance records list cannot be empty")
    @Valid
    private List<StaffAttendanceEntryRequest> records;

    public StaffAttendanceBatchImportRequest() {}

    public StaffAttendanceBatchImportRequest(String defaultDataSource, List<StaffAttendanceEntryRequest> records) {
        this.defaultDataSource = defaultDataSource;
        this.records = records;
    }

    public String getDefaultDataSource() { return defaultDataSource; }
    public void setDefaultDataSource(String defaultDataSource) { this.defaultDataSource = defaultDataSource; }

    public List<StaffAttendanceEntryRequest> getRecords() { return records; }
    public void setRecords(List<StaffAttendanceEntryRequest> records) { this.records = records; }
}
