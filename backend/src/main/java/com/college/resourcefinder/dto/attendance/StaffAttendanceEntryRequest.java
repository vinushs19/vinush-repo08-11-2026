package com.college.resourcefinder.dto.attendance;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public class StaffAttendanceEntryRequest {

    @NotBlank(message = "Staff identifier is required (e.g., STF-MCA-001)")
    @Size(max = 50, message = "Staff identifier must not exceed 50 characters")
    private String staffIdentifier;

    @NotBlank(message = "Staff name is required")
    @Size(max = 150, message = "Staff name must not exceed 150 characters")
    private String staffName;

    @NotNull(message = "Department ID is required")
    private Long departmentId;

    private Long userId;

    @NotNull(message = "Attendance date is required")
    @PastOrPresent(message = "Attendance date cannot be in the future")
    private LocalDate attendanceDate;

    @NotNull(message = "Session period is required")
    @Min(value = 1, message = "Session period must be between 1 and 8")
    @Max(value = 8, message = "Session period must be between 1 and 8")
    private Integer sessionPeriod = 1;

    @NotBlank(message = "Status is required")
    @Pattern(regexp = "^(PRESENT|ABSENT|ON_DUTY|LEAVE)$",
            message = "Status must be one of: PRESENT, ABSENT, ON_DUTY, LEAVE")
    private String status = "PRESENT";

    @Pattern(regexp = "^(MANUAL_ENTRY|CSV_IMPORT|BIOMETRIC_SYNC)$",
            message = "Data source must be one of: MANUAL_ENTRY, CSV_IMPORT, BIOMETRIC_SYNC")
    private String dataSource = "MANUAL_ENTRY";

    @Size(max = 255, message = "Remarks cannot exceed 255 characters")
    private String remarks;

    public StaffAttendanceEntryRequest() {}

    public StaffAttendanceEntryRequest(String staffIdentifier, String staffName, Long departmentId,
                                       Long userId, LocalDate attendanceDate, Integer sessionPeriod,
                                       String status, String dataSource, String remarks) {
        this.staffIdentifier = staffIdentifier;
        this.staffName = staffName;
        this.departmentId = departmentId;
        this.userId = userId;
        this.attendanceDate = attendanceDate;
        this.sessionPeriod = sessionPeriod;
        this.status = status;
        this.dataSource = dataSource;
        this.remarks = remarks;
    }

    // Getters and Setters
    public String getStaffIdentifier() { return staffIdentifier; }
    public void setStaffIdentifier(String staffIdentifier) { this.staffIdentifier = staffIdentifier; }

    public String getStaffName() { return staffName; }
    public void setStaffName(String staffName) { this.staffName = staffName; }

    public Long getDepartmentId() { return departmentId; }
    public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public LocalDate getAttendanceDate() { return attendanceDate; }
    public void setAttendanceDate(LocalDate attendanceDate) { this.attendanceDate = attendanceDate; }

    public Integer getSessionPeriod() { return sessionPeriod; }
    public void setSessionPeriod(Integer sessionPeriod) { this.sessionPeriod = sessionPeriod; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getDataSource() { return dataSource; }
    public void setDataSource(String dataSource) { this.dataSource = dataSource; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
}
