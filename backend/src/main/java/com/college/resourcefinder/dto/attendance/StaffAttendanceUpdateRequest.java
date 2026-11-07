package com.college.resourcefinder.dto.attendance;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class StaffAttendanceUpdateRequest {

    @Pattern(regexp = "^(PRESENT|ABSENT|ON_DUTY|LEAVE)$",
            message = "Status must be one of: PRESENT, ABSENT, ON_DUTY, LEAVE")
    private String status;

    @Size(max = 255, message = "Remarks cannot exceed 255 characters")
    private String remarks;

    public StaffAttendanceUpdateRequest() {}

    public StaffAttendanceUpdateRequest(String status, String remarks) {
        this.status = status;
        this.remarks = remarks;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
}
