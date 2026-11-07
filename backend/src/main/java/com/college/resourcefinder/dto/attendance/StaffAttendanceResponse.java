package com.college.resourcefinder.dto.attendance;

import com.college.resourcefinder.entity.StaffAttendance;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class StaffAttendanceResponse {

    private Long staffAttendanceId;
    private String staffIdentifier;
    private String staffName;
    private Long departmentId;
    private String departmentCode;
    private String departmentName;
    private Long userId;
    private String userEmail;
    private LocalDate attendanceDate;
    private Integer sessionPeriod;
    private String status;
    private String dataSource;
    private String remarks;
    private Long recordedById;
    private String recordedByName;
    private LocalDateTime recordedAt;
    private LocalDateTime updatedAt;

    public StaffAttendanceResponse() {}

    public static StaffAttendanceResponse fromEntity(StaffAttendance entity) {
        if (entity == null) return null;

        StaffAttendanceResponse resp = new StaffAttendanceResponse();
        resp.setStaffAttendanceId(entity.getStaffAttendanceId());
        resp.setStaffIdentifier(entity.getStaffIdentifier());
        resp.setStaffName(entity.getStaffName());

        if (entity.getDepartment() != null) {
            resp.setDepartmentId(entity.getDepartment().getDepartmentId());
            resp.setDepartmentCode(entity.getDepartment().getDepartmentCode());
            resp.setDepartmentName(entity.getDepartment().getDepartmentName());
        }

        if (entity.getUser() != null) {
            resp.setUserId(entity.getUser().getUserId());
            resp.setUserEmail(entity.getUser().getEmail());
        }

        resp.setAttendanceDate(entity.getAttendanceDate());
        resp.setSessionPeriod(entity.getSessionPeriod());
        resp.setStatus(entity.getStatus());
        resp.setDataSource(entity.getDataSource());
        resp.setRemarks(entity.getRemarks());

        if (entity.getRecordedBy() != null) {
            resp.setRecordedById(entity.getRecordedBy().getUserId());
            resp.setRecordedByName(entity.getRecordedBy().getName());
        }

        resp.setRecordedAt(entity.getRecordedAt());
        resp.setUpdatedAt(entity.getUpdatedAt());
        return resp;
    }

    // Getters and Setters
    public Long getStaffAttendanceId() { return staffAttendanceId; }
    public void setStaffAttendanceId(Long staffAttendanceId) { this.staffAttendanceId = staffAttendanceId; }

    public String getStaffIdentifier() { return staffIdentifier; }
    public void setStaffIdentifier(String staffIdentifier) { this.staffIdentifier = staffIdentifier; }

    public String getStaffName() { return staffName; }
    public void setStaffName(String staffName) { this.staffName = staffName; }

    public Long getDepartmentId() { return departmentId; }
    public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }

    public String getDepartmentCode() { return departmentCode; }
    public void setDepartmentCode(String departmentCode) { this.departmentCode = departmentCode; }

    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

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

    public Long getRecordedById() { return recordedById; }
    public void setRecordedById(Long recordedById) { this.recordedById = recordedById; }

    public String getRecordedByName() { return recordedByName; }
    public void setRecordedByName(String recordedByName) { this.recordedByName = recordedByName; }

    public LocalDateTime getRecordedAt() { return recordedAt; }
    public void setRecordedAt(LocalDateTime recordedAt) { this.recordedAt = recordedAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
